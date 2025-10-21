package com.paymenthub.payment_service.application.dto.result;

import com.paymenthub.payment_service.domain.entity.Payment;
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

    public static PaymentResult fromDomain(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getInvoiceId().getValue(),
                payment.getAuthorizedAmount().getAmount(),
                payment.getAuthorizedAmount().getCurrencyCode(),
                payment.getStatus(),
                payment.getCreatedAt());
    }
}
