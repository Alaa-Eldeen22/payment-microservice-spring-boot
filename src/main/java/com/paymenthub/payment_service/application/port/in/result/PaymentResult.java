package com.paymenthub.payment_service.application.port.in.result;

import com.paymenthub.payment_service.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public record PaymentResult(
        String paymentId,
        String invoiceId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDateTime createdAt) {
    public static PaymentResult from(
            String paymentId,
            String invoiceId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            LocalDateTime createdAt) {
        return new PaymentResult(paymentId, invoiceId, amount, currency, status, createdAt);
    }
}
