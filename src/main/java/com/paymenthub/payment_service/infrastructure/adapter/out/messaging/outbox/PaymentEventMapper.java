package com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paymenthub.payment_service.domain.events.DomainEvent;
import com.paymenthub.payment_service.domain.events.PaymentAuthorizedEvent;
import com.paymenthub.payment_service.domain.events.PaymentCapturedEvent;
import com.paymenthub.payment_service.domain.events.PaymentFailedEvent;
import com.paymenthub.payment_service.domain.events.PaymentVoidedEvent;
import com.paymenthub.payment_service.infrastructure.messaging.MessageContext;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;

import lombok.RequiredArgsConstructor;

/**
 * Domain events -> contract messages (§3.2). Events not in the contract map
 * to empty: with mandatory publishing they'd sit in the outbox forever.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventMapper {

    private final ObjectMapper objectMapper;

    public Optional<MessageEnvelope> toEnvelope(DomainEvent event, MessageContext.Data ctx) {
        Instant occurredAt = toInstant(event.getOccurredOn());
        ObjectNode payload = objectMapper.createObjectNode();
        String type;
        String paymentId;

        if (event instanceof PaymentAuthorizedEvent e) {
            type = PAYMENT_AUTHORIZED;
            paymentId = e.getPaymentId();
            payload.put("paymentId", paymentId)
                    .put("invoiceId", e.getInvoiceId())
                    .put("authorizedAt", occurredAt.toString());
        } else if (event instanceof PaymentFailedEvent e) {
            type = PAYMENT_FAILED;
            paymentId = e.getPaymentId();
            payload.put("paymentId", paymentId)
                    .put("invoiceId", e.getInvoiceId())
                    .put("reason", e.getReason())
                    .put("failedAt", occurredAt.toString());
        } else if (event instanceof PaymentCapturedEvent e) {
            type = PAYMENT_CAPTURED;
            paymentId = e.getPaymentId();
            Instant capturedAt = e.getCapturedAt() != null ? toInstant(e.getCapturedAt()) : occurredAt;
            payload.put("paymentId", paymentId)
                    .put("invoiceId", e.getInvoiceId())
                    .put("capturedAt", capturedAt.toString());
        } else if (event instanceof PaymentVoidedEvent e) {
            type = PAYMENT_VOIDED;
            paymentId = e.getPaymentId();
            payload.put("paymentId", paymentId)
                    .put("invoiceId", e.getInvoiceId())
                    .put("voidedAt", occurredAt.toString());
        } else {
            return Optional.empty();
        }

        return Optional.of(MessageEnvelope.create(
                type,
                type + ":" + paymentId,
                occurredAt,
                ctx.correlationId(),
                ctx.causationId(),
                payload));
    }

    // domain uses zone-less LocalDateTime in the JVM's zone; the wire is UTC
    private static Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
