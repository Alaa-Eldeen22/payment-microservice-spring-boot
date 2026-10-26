package com.paymenthub.payment_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;

/**
 * End-to-end on the payment side against real RabbitMQ + Postgres
 * (Testcontainers): envelope in on invoice.events, payment row + outbox,
 * payment.* envelope out on payment.events; retries, DLQ, duplicates.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "messaging.retry-delays-ms=300,300,300",
        "messaging.outbox.poll-interval-ms=200",
        "spring.jpa.show-sql=false" })
class MessagingFlowIntegrationTest {

    private static final String QUEUE = "payment-service.invoice-events";
    private static final String DLQ = QUEUE + ".dlq";
    /** stands in for the invoice service's queue on payment.events */
    private static final String PAYMENT_EVENTS_TAP = "test.payment-events-tap";

    @Autowired
    RabbitTemplate rabbit;
    @Autowired
    AmqpAdmin admin;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper mapper;

    @MockitoSpyBean
    CreateAndAuthorizePaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        bindTap();
        admin.purgeQueue(PAYMENT_EVENTS_TAP, false);
        admin.purgeQueue(DLQ, false);
        // invoices starting with "boom" always fail, "flaky" fails exactly once
        doThrow(new IllegalStateException("simulated outage")).when(useCase)
                .createAndAuthorize(argThat(c -> c != null && c.invoiceId().startsWith("boom")));
    }

    private void bindTap() {
        Queue tap = new Queue(PAYMENT_EVENTS_TAP, true);
        admin.declareQueue(tap);
        admin.declareBinding(BindingBuilder.bind(tap).to(new TopicExchange("payment.events")).with("payment.#"));
    }

    // ---------------------------------------------------------------- helpers

    private String invoiceCreated(String messageId, String invoiceId, String paymentMethodId) {
        return """
                {"messageId":"%s","idempotencyKey":"invoice.created:%s","eventType":"invoice.created",
                 "schemaVersion":1,"occurredAt":"%s","correlationId":"corr-%s","causationId":null,
                 "producer":"invoice-service",
                 "payload":{"invoiceId":"%s","customerId":"cust-1","amount":"20.20","currency":"USD",
                            "dueDate":"2027-01-01T00:00:00.000Z","itemCount":1,"paymentMethodId":"%s"}}
                """.formatted(messageId, invoiceId, Instant.now(), invoiceId, invoiceId, paymentMethodId);
    }

    private void send(String body, String messageId) {
        MessageProperties p = new MessageProperties();
        p.setMessageId(messageId);
        p.setContentType("application/json");
        rabbit.send("invoice.events", "invoice.created", new Message(body.getBytes(StandardCharsets.UTF_8), p));
    }

    private static void await(String what, Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            if (Boolean.TRUE.equals(condition.get())) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("timed out waiting for: " + what);
    }

    private JsonNode receiveEnvelope(String queue) throws Exception {
        Message m = rabbit.receive(queue, 15_000);
        assertThat(m).as("message on " + queue).isNotNull();
        return mapper.readTree(m.getBody());
    }

    private int payments(String invoiceId) {
        return jdbc.queryForObject("SELECT count(*) FROM payments WHERE invoice_id = ?", Integer.class, invoiceId);
    }

    // ------------------------------------------------------------------ tests

    @Test
    void invoiceCreatedLeadsToAuthorizedPaymentAndPaymentAuthorizedEnvelope() throws Exception {
        String invoiceId = "inv-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();

        send(invoiceCreated(messageId, invoiceId, "pm_ok"), messageId);

        JsonNode out = receiveEnvelope(PAYMENT_EVENTS_TAP);
        assertThat(out.get("eventType").asText()).isEqualTo("payment.authorized");
        assertThat(out.get("schemaVersion").asInt()).isEqualTo(1);
        assertThat(out.get("producer").asText()).isEqualTo("payment-service");
        assertThat(out.get("correlationId").asText()).isEqualTo("corr-" + invoiceId);
        assertThat(out.get("causationId").asText()).isEqualTo(messageId);
        assertThat(out.get("occurredAt").asText()).endsWith("Z");
        JsonNode payload = out.get("payload");
        assertThat(payload.get("invoiceId").asText()).isEqualTo(invoiceId);
        assertThat(payload.get("authorizedAt").asText()).endsWith("Z");
        assertThat(out.get("idempotencyKey").asText())
                .isEqualTo("payment.authorized:" + payload.get("paymentId").asText());

        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE invoice_id = ?", String.class, invoiceId))
                .isEqualTo("AUTHORIZED");
        await("outbox row published", () -> jdbc.queryForObject(
                "SELECT count(*) FROM outbox_messages WHERE id = ?::uuid AND published_at IS NOT NULL",
                Integer.class, out.get("messageId").asText()) == 1);
    }

    @Test
    void duplicateDeliveryIsProcessedOnce() throws Exception {
        String invoiceId = "inv-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();
        String body = invoiceCreated(messageId, invoiceId, "pm_ok");

        send(body, messageId);
        send(body, messageId);
        receiveEnvelope(PAYMENT_EVENTS_TAP);

        await("both deliveries consumed", () -> admin.getQueueInfo(QUEUE).getMessageCount() == 0);
        Thread.sleep(1000);
        assertThat(payments(invoiceId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_messages WHERE idempotency_key = ?",
                Integer.class, "invoice.created:" + invoiceId)).isEqualTo(1);
        assertThat(rabbit.receive(PAYMENT_EVENTS_TAP, 1000)).as("no second payment event").isNull();
    }

    @Test
    void gatewayDeclineCommitsFailedPaymentAndPublishesPaymentFailed() throws Exception {
        String invoiceId = "inv-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();

        send(invoiceCreated(messageId, invoiceId, "fail"), messageId);

        JsonNode out = receiveEnvelope(PAYMENT_EVENTS_TAP);
        assertThat(out.get("eventType").asText()).isEqualTo("payment.failed");
        assertThat(out.at("/payload/reason").asText()).contains("decline");
        assertThat(out.at("/payload/failedAt").asText()).endsWith("Z");
        // used to be rolled back along with the PENDING row
        assertThat(jdbc.queryForObject("SELECT status FROM payments WHERE invoice_id = ?", String.class, invoiceId))
                .isEqualTo("FAILED");
    }

    @Test
    void repeatedFailureRetriesWithBackoffThenLandsInDlq() throws Exception {
        String invoiceId = "boom-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();
        String body = invoiceCreated(messageId, invoiceId, "pm_ok");

        send(body, messageId);

        Message dead = rabbit.receive(DLQ, 15_000);
        assertThat(dead).isNotNull();
        Map<String, Object> h = dead.getMessageProperties().getHeaders();
        assertThat(h).containsEntry("x-dlq-reason", "max_retries_exceeded")
                .containsEntry("x-dlq-consumer", "payment-service")
                .containsEntry("x-dlq-queue", QUEUE)
                .containsEntry("x-original-exchange", "invoice.events")
                .containsEntry("x-original-routing-key", "invoice.created");
        assertThat(((Number) h.get("x-dlq-attempts")).intValue()).isEqualTo(4);
        assertThat(((Number) h.get("x-retry-count")).intValue()).isEqualTo(3);
        assertThat(String.valueOf(h.get("x-dlq-error"))).contains("simulated outage");
        assertThat(dead.getMessageProperties().getMessageId()).isEqualTo(messageId);
        assertThat(dead.getMessageProperties().getReceivedRoutingKey()).isEqualTo("invoice.created");
        // body travels byte-for-byte through retries and into the DLQ
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo(body);

        assertThat(payments(invoiceId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_messages WHERE idempotency_key = ?",
                Integer.class, "invoice.created:" + invoiceId)).isZero();
    }

    @Test
    void poisonMessageGoesToDlqAndConsumerKeepsWorking() throws Exception {
        String poisonId = UUID.randomUUID().toString();
        send("{this is not json", poisonId);

        Message dead = rabbit.receive(DLQ, 15_000);
        assertThat(dead).isNotNull();
        assertThat(dead.getMessageProperties().getHeaders()).containsEntry("x-dlq-reason", "malformed_message");
        assertThat(dead.getMessageProperties().getMessageId()).isEqualTo(poisonId);

        String invoiceId = "inv-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();
        send(invoiceCreated(messageId, invoiceId, "pm_ok"), messageId);
        assertThat(receiveEnvelope(PAYMENT_EVENTS_TAP).get("eventType").asText()).isEqualTo("payment.authorized");
    }

    @Test
    void outboxHoldsUnroutableEventsUntilAQueueIsBound() throws Exception {
        admin.deleteQueue(PAYMENT_EVENTS_TAP); // nothing bound to payment.* any more
        String invoiceId = "inv-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();

        send(invoiceCreated(messageId, invoiceId, "pm_ok"), messageId);

        await("outbox row rejected as unroutable", () -> Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT count(*) > 0 FROM outbox_messages
                WHERE correlation_id = ? AND published_at IS NULL AND last_error LIKE 'UnroutableMessageException%'
                """, Boolean.class, "corr-" + invoiceId)));

        bindTap();

        JsonNode out = receiveEnvelope(PAYMENT_EVENTS_TAP);
        assertThat(out.at("/payload/invoiceId").asText()).isEqualTo(invoiceId);
        assertThat(jdbc.queryForObject("""
                SELECT attempts FROM outbox_messages WHERE id = ?::uuid
                """, Integer.class, out.get("messageId").asText())).isGreaterThan(1);
    }

    @Test
    void transientFailureIsRetriedAndProcessedExactlyOnce() throws Exception {
        String invoiceId = "flaky-" + UUID.randomUUID();
        String messageId = UUID.randomUUID().toString();
        doThrow(new IllegalStateException("blip")).doCallRealMethod().when(useCase)
                .createAndAuthorize(argThat(c -> c != null && c.invoiceId().equals(invoiceId)));

        send(invoiceCreated(messageId, invoiceId, "pm_ok"), messageId);

        JsonNode out = receiveEnvelope(PAYMENT_EVENTS_TAP);
        assertThat(out.get("eventType").asText()).isEqualTo("payment.authorized");
        assertThat(payments(invoiceId)).isEqualTo(1);
        assertThat(rabbit.receive(DLQ, 500)).isNull();
    }
}
