package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.*;
import static com.paymenthub.payment_service.infrastructure.messaging.MessagingLog.fields;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox.ProcessedMessageStore;
import com.paymenthub.payment_service.infrastructure.messaging.ConfirmingPublisher;
import com.paymenthub.payment_service.infrastructure.messaging.MessageContext;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.Headers;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.MalformedMessageException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.NonRetryableMessageException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.UnknownEventTypeException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.UnsupportedSchemaVersionException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingLog;

/**
 * Implements the consumer algorithm from MESSAGING-CONTRACT.md §4.1. Never
 * throws: every path ends in ACK (processed / duplicate / handed to retry or
 * DLQ with a confirmed publish) or REQUEUE (hand-off failed).
 */
public class MessageProcessor {

    public enum Outcome {
        ACK, REQUEUE
    }

    public record Config(
            String queue,
            Map<String, EventHandler> handlers,
            int maxRetries,
            /** extra non-retryable classification on top of NonRetryableMessageException */
            Predicate<Throwable> nonRetryable,
            /** business "nothing to do" outcomes: ack without retrying */
            Predicate<Throwable> noOp) {
    }

    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    private final Config config;
    private final TransactionTemplate tx;
    private final ProcessedMessageStore dedup;
    private final ConfirmingPublisher publisher;
    private final ObjectMapper objectMapper;

    public MessageProcessor(Config config, TransactionTemplate tx, ProcessedMessageStore dedup,
            ConfirmingPublisher publisher, ObjectMapper objectMapper) {
        this.config = config;
        this.tx = tx;
        this.dedup = dedup;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    public Outcome process(Message msg) {
        MessageProperties props = msg.getMessageProperties();
        int retryCount = toInt(props.getHeaders().get(Headers.RETRY_COUNT));
        Map<String, Object> f = fields(
                "queue", config.queue(),
                "messageId", props.getMessageId(),
                "correlationId", props.getCorrelationId(),
                "routingKey", props.getReceivedRoutingKey(),
                "retryCount", retryCount,
                "redelivered", props.isRedelivered());
        MDC.put("messageId", String.valueOf(props.getMessageId()));
        try {
            MessageEnvelope envelope;
            EventHandler handler;
            try {
                envelope = MessageEnvelope.parse(msg.getBody(), objectMapper);
                f.put("messageId", envelope.messageId());
                f.put("correlationId", envelope.correlationId());
                f.put("eventType", envelope.eventType());
                f.put("idempotencyKey", envelope.idempotencyKey());
                MDC.put("messageId", envelope.messageId());
                handler = config.handlers().get(envelope.eventType());
                if (handler == null) {
                    throw new UnknownEventTypeException("no handler for " + envelope.eventType());
                }
                if (!handler.versions().contains(envelope.schemaVersion())) {
                    throw new UnsupportedSchemaVersionException(envelope.eventType() + " v"
                            + envelope.schemaVersion() + " not supported (supported: " + handler.versions() + ")");
                }
                handler.validate(envelope.payload(), envelope.schemaVersion());
            } catch (RuntimeException e) {
                return deadLetter(msg, contractReason(e), e, f);
            }

            MessagingLog.log(log, Level.INFO, "rmq.consume.received", f);
            return handle(msg, envelope, handler, retryCount, f);
        } finally {
            MDC.remove("messageId");
        }
    }

    private Outcome handle(Message msg, MessageEnvelope envelope, EventHandler handler, int retryCount,
            Map<String, Object> f) {
        try {
            Boolean duplicate = MessageContext.run(
                    new MessageContext.Data(envelope.correlationId(), envelope.messageId()),
                    () -> tx.execute(status -> {
                        if (!dedup.tryRecord(config.queue(), envelope)) {
                            return true;
                        }
                        handler.handle(envelope);
                        return false;
                    }));
            MessagingLog.log(log, Level.INFO,
                    Boolean.TRUE.equals(duplicate) ? "rmq.consume.duplicate" : "rmq.consume.ok", f);
            return Outcome.ACK;
        } catch (RuntimeException e) {
            if (config.noOp() != null && config.noOp().test(e)) {
                // tx rolled back, nothing to do: e.g. an active payment already exists
                f.put("noop", MessagingLog.describe(e));
                MessagingLog.log(log, Level.INFO, "rmq.consume.ok", f);
                return Outcome.ACK;
            }
            if (e instanceof NonRetryableMessageException
                    || (config.nonRetryable() != null && config.nonRetryable().test(e))) {
                return deadLetter(msg, "non_retryable_error", e, f);
            }
            if (retryCount < config.maxRetries()) {
                return scheduleRetry(msg, retryCount + 1, e, f);
            }
            return deadLetter(msg, "max_retries_exceeded", e, f);
        }
    }

    private Outcome scheduleRetry(Message msg, int step, Throwable err, Map<String, Object> f) {
        String error = MessagingLog.describe(err);
        Map<String, Object> headers = failureHeaders(msg, error);
        headers.put(Headers.RETRY_COUNT, step);
        try {
            publisher.publish(retryExchange(config.queue()), String.valueOf(step), msg.getBody(),
                    copyProperties(msg, headers));
        } catch (RuntimeException publishErr) {
            return requeue(f, error, publishErr);
        }
        f.put("retryCount", step);
        f.put("error", error);
        MessagingLog.log(log, Level.WARN, "rmq.consume.retry_scheduled", f);
        return Outcome.ACK;
    }

    private Outcome deadLetter(Message msg, String reason, Throwable err, Map<String, Object> f) {
        String error = MessagingLog.describe(err);
        Map<String, Object> headers = failureHeaders(msg, error);
        int attempts = toInt(headers.get(Headers.RETRY_COUNT)) + 1;
        headers.put(Headers.DLQ_REASON, reason);
        headers.put(Headers.DLQ_ERROR, error);
        headers.put(Headers.DLQ_ATTEMPTS, attempts);
        headers.put(Headers.DLQ_QUEUE, config.queue());
        headers.put(Headers.DLQ_CONSUMER, SERVICE_NAME);
        headers.put(Headers.DLQ_AT, Instant.now().toString());
        try {
            publisher.publish(deadLetterExchange(config.queue()),
                    String.valueOf(headers.get(Headers.ORIGINAL_ROUTING_KEY)), msg.getBody(),
                    copyProperties(msg, headers));
        } catch (RuntimeException publishErr) {
            return requeue(f, error, publishErr);
        }
        f.put("reason", reason);
        f.put("attempts", attempts);
        f.put("error", error);
        MessagingLog.log(log, Level.ERROR, "rmq.consume.dead_lettered", f);
        return Outcome.ACK;
    }

    private Outcome requeue(Map<String, Object> f, String error, Throwable publishErr) {
        f.put("error", error);
        f.put("handOffError", MessagingLog.describe(publishErr));
        MessagingLog.log(log, Level.ERROR, "rmq.consume.requeued", f);
        return Outcome.REQUEUE;
    }

    /** existing headers + the §4.3 bookkeeping (original-* set only once) */
    private Map<String, Object> failureHeaders(Message msg, String error) {
        MessageProperties props = msg.getMessageProperties();
        Map<String, Object> headers = new HashMap<>(props.getHeaders());
        headers.putIfAbsent(Headers.ORIGINAL_EXCHANGE, nullToEmpty(props.getReceivedExchange()));
        headers.putIfAbsent(Headers.ORIGINAL_ROUTING_KEY, nullToEmpty(props.getReceivedRoutingKey()));
        headers.putIfAbsent(Headers.FIRST_FAILED_AT, Instant.now().toString());
        headers.put(Headers.LAST_ERROR, error);
        return headers;
    }

    /** Copy keeps body + identity properties, only headers change (§4.1). */
    private static MessageProperties copyProperties(Message msg, Map<String, Object> headers) {
        MessageProperties in = msg.getMessageProperties();
        MessageProperties out = new MessageProperties();
        // malformed input may lack one; the publisher needs it to track returns
        out.setMessageId(in.getMessageId() != null ? in.getMessageId() : UUID.randomUUID().toString());
        out.setCorrelationId(in.getCorrelationId());
        out.setType(in.getType());
        out.setAppId(in.getAppId());
        out.setContentType(in.getContentType() != null ? in.getContentType() : MessageProperties.CONTENT_TYPE_JSON);
        out.setContentEncoding(in.getContentEncoding());
        out.setTimestamp(in.getTimestamp());
        out.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        headers.forEach(out::setHeader);
        return out;
    }

    private static String contractReason(RuntimeException e) {
        if (e instanceof UnknownEventTypeException) {
            return "unknown_event_type";
        }
        if (e instanceof UnsupportedSchemaVersionException) {
            return "unsupported_schema_version";
        }
        if (e instanceof MalformedMessageException) {
            return "malformed_message";
        }
        return "malformed_message";
    }

    static int toInt(Object value) {
        if (value instanceof Number n) {
            return Math.max(0, n.intValue());
        }
        if (value != null) {
            try {
                return Math.max(0, Integer.parseInt(value.toString().trim()));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
