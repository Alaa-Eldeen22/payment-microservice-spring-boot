package com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingLog.fields;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.event.Level;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.infrastructure.messaging.ConfirmingPublisher;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.Headers;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.UnroutableMessageException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingLog;

import lombok.extern.slf4j.Slf4j;

/**
 * Publishes pending outbox rows (contract §7). FOR UPDATE SKIP LOCKED makes
 * it safe to run on several instances.
 */
@Slf4j
@Component
public class OutboxRelay {

    private static final long MAX_BACKOFF_S = 60;

    private final OutboxMessageRepository repository;
    private final ConfirmingPublisher publisher;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public OutboxRelay(OutboxMessageRepository repository, ConfirmingPublisher publisher,
            TransactionTemplate tx, ObjectMapper objectMapper,
            @Value("${messaging.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.publisher = publisher;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.poll-interval-ms:1000}")
    public void poll() {
        try {
            // keep draining while full batches go out
            while (relayOnce() == batchSize) {
            }
        } catch (RuntimeException e) {
            log.error("outbox relay tick failed: {}", MessagingLog.describe(e));
        }
    }

    /** Publishes one batch, returns how many rows were published. */
    public int relayOnce() {
        Integer published = tx.execute(status -> {
            List<OutboxMessageEntity> rows = repository.lockPendingBatch(batchSize);
            int ok = 0;
            for (OutboxMessageEntity row : rows) {
                boolean sent = publishRow(row, Instant.now());
                repository.save(row);
                // stop at the first failure, skipping ahead would reorder events
                if (!sent) {
                    break;
                }
                ok++;
            }
            return ok;
        });
        return published == null ? 0 : published;
    }

    /** Publishes a single row and updates its bookkeeping fields in place. */
    boolean publishRow(OutboxMessageEntity row, Instant now) {
        Map<String, Object> f = fields(
                "messageId", row.getId(),
                "correlationId", row.getCorrelationId(),
                "eventType", row.getEventType(),
                "exchange", row.getExchange(),
                "routingKey", row.getRoutingKey(),
                "attempt", row.getAttempts() + 1);
        try {
            publisher.publish(row.getExchange(), row.getRoutingKey(),
                    row.getBody().getBytes(StandardCharsets.UTF_8), properties(row));
            row.setAttempts(row.getAttempts() + 1);
            row.setPublishedAt(now);
            row.setLastError(null);
            MessagingLog.log(log, Level.INFO, "rmq.publish.ok", f);
            return true;
        } catch (RuntimeException e) {
            row.setAttempts(row.getAttempts() + 1);
            long backoffS = Math.min(1L << Math.min(row.getAttempts(), 30), MAX_BACKOFF_S);
            row.setNextAttemptAt(now.plusSeconds(backoffS));
            row.setLastError(MessagingLog.describe(e));
            boolean unroutable = e instanceof UnroutableMessageException;
            f.put("error", row.getLastError());
            f.put("nextAttemptInS", backoffS);
            MessagingLog.log(log, unroutable ? Level.ERROR : Level.WARN,
                    unroutable ? "rmq.publish.returned" : "rmq.publish.failed", f);
            return false;
        }
    }

    private MessageProperties properties(OutboxMessageEntity row) {
        MessageProperties p = new MessageProperties();
        p.setMessageId(row.getId().toString());
        p.setCorrelationId(row.getCorrelationId());
        p.setType(row.getEventType());
        p.setAppId(com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.SERVICE_NAME);
        p.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        p.setContentEncoding("utf-8");
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        try {
            JsonNode env = objectMapper.readTree(row.getBody());
            p.setTimestamp(Date.from(Instant.parse(env.get("occurredAt").asText())));
            p.setHeader(Headers.SCHEMA_VERSION, env.get("schemaVersion").asInt());
        } catch (Exception e) {
            // our own body, should never happen; publish without the extras
            log.warn("could not read envelope of outbox row {}: {}", row.getId(), e.getMessage());
        }
        return p;
    }
}
