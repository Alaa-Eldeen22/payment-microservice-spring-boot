package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer.MessageProcessor.Outcome;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox.ProcessedMessageStore;
import com.paymenthub.payment_service.infrastructure.messaging.ConfirmingPublisher;
import com.paymenthub.payment_service.infrastructure.messaging.MessageContext;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.MalformedMessageException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.PublishFailedException;

class MessageProcessorTest {

    private static final String QUEUE = "payment-service.invoice-events";
    private static final String MESSAGE_ID = "6f1c1f0e-8a8b-4a53-9d0e-4d2b7d0c9a11";

    private final ObjectMapper mapper = new ObjectMapper();
    private ConfirmingPublisher publisher;
    private ProcessedMessageStore dedup;
    private EventHandler handler;
    private MessageProcessor processor;
    private Set<String> recorded;

    @BeforeEach
    void setUp() {
        publisher = mock(ConfirmingPublisher.class);
        dedup = mock(ProcessedMessageStore.class);
        handler = mock(EventHandler.class);
        when(handler.versions()).thenReturn(Set.of(1));

        recorded = new HashSet<>();
        when(dedup.tryRecord(anyString(), any())).thenAnswer(inv -> {
            MessageEnvelope env = inv.getArgument(1);
            return recorded.add(env.idempotencyKey());
        });

        // fake tx: undo the dedup insert when the work throws
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> {
            Set<String> before = new HashSet<>(recorded);
            try {
                return ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null);
            } catch (RuntimeException e) {
                recorded = before;
                throw e;
            }
        });

        processor = new MessageProcessor(
                new MessageProcessor.Config(QUEUE, Map.of("invoice.created", handler), 3,
                        e -> e instanceof IllegalArgumentException,
                        e -> e instanceof DuplicatePaymentException),
                tx, dedup, publisher, mapper);
    }

    private static String envelope(String eventType, int version) {
        return """
                {"messageId":"%s","idempotencyKey":"invoice.created:inv-1","eventType":"%s",
                 "schemaVersion":%d,"occurredAt":"2026-10-26T14:03:12.481Z","correlationId":"corr-1",
                 "causationId":null,"producer":"invoice-service",
                 "payload":{"invoiceId":"inv-1","amount":"20.20"}}
                """.formatted(MESSAGE_ID, eventType, version);
    }

    private static Message delivery(String body, Map<String, Object> headers, String exchange, String routingKey) {
        MessageProperties p = new MessageProperties();
        p.setMessageId(MESSAGE_ID);
        p.setCorrelationId("corr-1");
        p.setContentType("application/json");
        p.setReceivedExchange(exchange);
        p.setReceivedRoutingKey(routingKey);
        headers.forEach(p::setHeader);
        return new Message(body.getBytes(StandardCharsets.UTF_8), p);
    }

    private static Message delivery(String body) {
        return delivery(body, Map.of(), "invoice.events", "invoice.created");
    }

    private MessageProperties publishedProps(String exchange, String routingKey) {
        ArgumentCaptor<MessageProperties> props = ArgumentCaptor.forClass(MessageProperties.class);
        verify(publisher).publish(eq(exchange), eq(routingKey), any(), props.capture());
        return props.getValue();
    }

    @Test
    void processesValidMessageAndAcks() {
        assertThat(processor.process(delivery(envelope("invoice.created", 1)))).isEqualTo(Outcome.ACK);

        verify(handler).handle(any());
        verify(dedup).tryRecord(eq(QUEUE), any());
        verify(publisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void handlerRunsWithInboundCorrelationAndCausation() {
        AtomicReference<MessageContext.Data> seen = new AtomicReference<>();
        doAnswer(inv -> {
            seen.set(MessageContext.current().orElse(null));
            return null;
        }).when(handler).handle(any());

        processor.process(delivery(envelope("invoice.created", 1)));

        assertThat(seen.get()).isEqualTo(new MessageContext.Data("corr-1", MESSAGE_ID));
        assertThat(MessageContext.current()).isEmpty();
    }

    @Test
    void ignoresDuplicateDelivery() {
        processor.process(delivery(envelope("invoice.created", 1)));
        Outcome second = processor.process(delivery(envelope("invoice.created", 1)));

        assertThat(second).isEqualTo(Outcome.ACK);
        verify(handler, times(1)).handle(any());
        verify(publisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void schedulesRetryOnFailureAndAcks() {
        doThrow(new IllegalStateException("db is down")).when(handler).handle(any());

        Outcome outcome = processor.process(delivery(envelope("invoice.created", 1)));

        assertThat(outcome).isEqualTo(Outcome.ACK);
        MessageProperties p = publishedProps(QUEUE + ".retry", "1");
        assertThat(p.getMessageId()).isEqualTo(MESSAGE_ID);
        assertThat(p.getCorrelationId()).isEqualTo("corr-1");
        assertThat(p.getHeaders())
                .containsEntry("x-retry-count", 1)
                .containsEntry("x-original-exchange", "invoice.events")
                .containsEntry("x-original-routing-key", "invoice.created")
                .containsEntry("x-last-error", "IllegalStateException: db is down")
                .containsKey("x-first-failed-at");
    }

    @Test
    void failedAttemptDoesNotLeaveDedupRecord() {
        doThrow(new IllegalStateException("transient")).doNothing().when(handler).handle(any());

        processor.process(delivery(envelope("invoice.created", 1)));
        Outcome retried = processor.process(delivery(envelope("invoice.created", 1),
                Map.of("x-retry-count", 1), "", QUEUE));

        assertThat(retried).isEqualTo(Outcome.ACK);
        verify(handler, times(2)).handle(any());
        verify(publisher, times(1)).publish(any(), any(), any(), any());
    }

    @Test
    void retryCountFromNodeMayArriveAsLong() {
        doThrow(new IllegalStateException("x")).when(handler).handle(any());

        processor.process(delivery(envelope("invoice.created", 1),
                Map.of("x-retry-count", 2L, "x-original-routing-key", "invoice.created"), "", QUEUE));

        assertThat(publishedProps(QUEUE + ".retry", "3").getHeaders()).containsEntry("x-retry-count", 3);
    }

    @Test
    void deadLettersAfterRetriesExhausted() {
        doThrow(new IllegalStateException("never works")).when(handler).handle(any());

        Outcome outcome = processor.process(delivery(envelope("invoice.created", 1),
                Map.of("x-retry-count", 3, "x-original-exchange", "invoice.events",
                        "x-original-routing-key", "invoice.created"),
                "", QUEUE));

        assertThat(outcome).isEqualTo(Outcome.ACK);
        MessageProperties p = publishedProps(QUEUE + ".dlx", "invoice.created");
        assertThat(p.getHeaders())
                .containsEntry("x-dlq-reason", "max_retries_exceeded")
                .containsEntry("x-dlq-error", "IllegalStateException: never works")
                .containsEntry("x-dlq-attempts", 4)
                .containsEntry("x-dlq-queue", QUEUE)
                .containsEntry("x-dlq-consumer", "payment-service")
                .containsKey("x-dlq-at");
    }

    @Test
    void deadLettersNonRetryableImmediately() {
        doThrow(new IllegalArgumentException("bad currency")).when(handler).handle(any());

        processor.process(delivery(envelope("invoice.created", 1)));

        MessageProperties p = publishedProps(QUEUE + ".dlx", "invoice.created");
        assertThat(p.getHeaders()).containsEntry("x-dlq-reason", "non_retryable_error")
                .containsEntry("x-dlq-attempts", 1);
    }

    @Test
    void businessNoOpIsAckedWithoutRetry() {
        doThrow(new DuplicatePaymentException("active payment exists")).when(handler).handle(any());

        assertThat(processor.process(delivery(envelope("invoice.created", 1)))).isEqualTo(Outcome.ACK);
        verify(publisher, never()).publish(any(), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{nope|malformed_message",
            "{\"eventType\":\"invoice.created\"}|malformed_message",
            "UNKNOWN|unknown_event_type",
            "V2|unsupported_schema_version" })
    void deadLettersPoisonMessages(String body, String reason) {
        String actual = switch (body) {
            case "UNKNOWN" -> envelope("invoice.deleted", 1);
            case "V2" -> envelope("invoice.created", 2);
            default -> body;
        };

        Outcome outcome = processor.process(delivery(actual));

        assertThat(outcome).isEqualTo(Outcome.ACK);
        verify(handler, never()).handle(any());
        assertThat(publishedProps(QUEUE + ".dlx", "invoice.created").getHeaders())
                .containsEntry("x-dlq-reason", reason);
    }

    @Test
    void deadLettersWhenPayloadValidationFails() {
        doThrow(new MalformedMessageException("payload.amount missing")).when(handler)
                .validate(any(JsonNode.class), eq(1));

        processor.process(delivery(envelope("invoice.created", 1)));

        verify(handler, never()).handle(any());
        assertThat(publishedProps(QUEUE + ".dlx", "invoice.created").getHeaders())
                .containsEntry("x-dlq-reason", "malformed_message");
    }

    @Test
    void requeuesWhenRetryHandOffFails() {
        doThrow(new IllegalStateException("boom")).when(handler).handle(any());
        doThrow(new PublishFailedException("broker down", null)).when(publisher).publish(any(), any(), any(), any());

        assertThat(processor.process(delivery(envelope("invoice.created", 1)))).isEqualTo(Outcome.REQUEUE);
    }

    @Test
    void requeuesWhenDlqHandOffFails() {
        doThrow(new PublishFailedException("broker down", null)).when(publisher).publish(any(), any(), any(), any());

        assertThat(processor.process(delivery("{nope"))).isEqualTo(Outcome.REQUEUE);
    }
}
