
package com.paymenthub.payment_service.application.dto.response;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.paymenthub.payment_service.domain.entity.Payment;

@Value
@Builder
public class PaymentResponse {
    String id;
    String invoiceId;
    BigDecimal authorizedAmount;
    BigDecimal capturedAmount;
    BigDecimal remainingAmount;
    String currency;
    String status;
    String stripePaymentIntentId;
    LocalDateTime createdAt;
    LocalDateTime authorizedAt;
    LocalDateTime capturedAt;
    LocalDateTime expiresAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoiceId().getValue())
                .authorizedAmount(payment.getAuthorizedAmount().getAmount())
                .capturedAmount(payment.getCapturedAmount().getAmount())
                .remainingAmount(payment.getRemainingAuthorizationAmount().getAmount())
                .currency(payment.getAuthorizedAmount().getCurrencyCode())
                .status(payment.getStatus().name())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .createdAt(payment.getCreatedAt())
                .authorizedAt(payment.getAuthorizedAt())
                .capturedAt(payment.getCapturedAt())
                .expiresAt(payment.getExpiresAt())
                .build();
    }
}