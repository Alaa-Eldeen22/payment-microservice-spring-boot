package com.paymenthub.payment_service.infrastructure.adapter.in.response;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;

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
    LocalDateTime authorizedAt;
    LocalDateTime capturedAt;
    LocalDateTime expiresAt;

    public static PaymentResponse fromResult(PaymentResult result) {
        return PaymentResponse.builder()
                .id(result.paymentId())
                .invoiceId(result.invoiceId())
                .authorizedAmount(result.amount())
                .currency(result.currency())
                .status(result.status().name())
                .build();
    }
}