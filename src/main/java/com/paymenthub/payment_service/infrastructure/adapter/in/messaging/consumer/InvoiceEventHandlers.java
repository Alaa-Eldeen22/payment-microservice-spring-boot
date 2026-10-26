package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.INVOICE_CREATED;
import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.INVOICE_RETRIED;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.MalformedMessageException;

/** invoice.* handlers, contract §3.2 (v1 payloads). */
public final class InvoiceEventHandlers {

    private InvoiceEventHandlers() {
    }

    public static Map<String, EventHandler> create(CreateAndAuthorizePaymentUseCase useCase) {
        return Map.of(
                INVOICE_CREATED, new CreateAndAuthorizeHandler(useCase, true),
                INVOICE_RETRIED, new CreateAndAuthorizeHandler(useCase, false));
    }

    /** Both events start a payment attempt; created carries a few extra fields. */
    private record CreateAndAuthorizeHandler(CreateAndAuthorizePaymentUseCase useCase, boolean created)
            implements EventHandler {

        @Override
        public Set<Integer> versions() {
            return Set.of(1);
        }

        @Override
        public void validate(JsonNode p, int version) {
            for (String field : new String[] { "invoiceId", "customerId", "currency", "paymentMethodId" }) {
                MessageEnvelope.requireText(p, "payload." + field, field, 200);
            }
            decimal(p);
            if (created) {
                MessageEnvelope.requireTimestamp(
                        MessageEnvelope.requireText(p, "payload.dueDate", "dueDate", 100), "payload.dueDate");
                if (!p.path("itemCount").isIntegralNumber()) {
                    throw new MalformedMessageException("payload.itemCount must be an integer");
                }
            }
        }

        @Override
        public void handle(MessageEnvelope envelope) {
            JsonNode p = envelope.payload();
            useCase.createAndAuthorize(new CreatePaymentCommand(
                    p.get("invoiceId").asText(),
                    p.get("customerId").asText(),
                    decimal(p),
                    p.get("currency").asText(),
                    p.get("paymentMethodId").asText()));
        }

        // contract §2: amounts are decimal strings, never JSON numbers
        private static BigDecimal decimal(JsonNode p) {
            String raw = MessageEnvelope.requireText(p, "payload.amount", "amount", 50);
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException e) {
                throw new MalformedMessageException("payload.amount must be a decimal string");
            }
        }
    }
}
