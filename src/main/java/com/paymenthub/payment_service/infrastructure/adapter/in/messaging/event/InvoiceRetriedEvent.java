package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event;

import java.math.BigDecimal;

public record InvoiceRetriedEvent(
                String eventId,
                String invoiceId,
                String customerId,
                BigDecimal amount,
                String currency,
                String paymentMethodId) {
}
