package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceCreatedEvent(
        String eventId,
        String invoiceId,
        String customerId,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt) {
}
