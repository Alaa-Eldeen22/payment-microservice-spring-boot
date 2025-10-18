package com.paymenthub.payment_service.application.port.out;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class RefundResult {
    boolean success;
    String refundTransactionId;
    String originalTransactionId;
    BigDecimal refundedAmount;
    String currency;
    LocalDateTime refundedAt;
    String errorMessage;
    String errorCode;
    String gatewayResponseCode;
    String gatewayMessage;

    // For partial refunds
    boolean partialRefund;
    BigDecimal remainingRefundableAmount;

    // Gateway-specific data
    java.util.Map<String, Object> gatewayMetadata;

    public static RefundResult success(String refundTransactionId, String originalTransactionId,
            BigDecimal refundedAmount, String currency) {
        return RefundResult.builder()
                .success(true)
                .refundTransactionId(refundTransactionId)
                .originalTransactionId(originalTransactionId)
                .refundedAmount(refundedAmount)
                .currency(currency)
                .refundedAt(LocalDateTime.now())
                .build();
    }

    public static RefundResult success(String refundTransactionId, String originalTransactionId,
            BigDecimal refundedAmount, String currency,
            boolean partialRefund, BigDecimal remainingAmount) {
        return RefundResult.builder()
                .success(true)
                .refundTransactionId(refundTransactionId)
                .originalTransactionId(originalTransactionId)
                .refundedAmount(refundedAmount)
                .currency(currency)
                .refundedAt(LocalDateTime.now())
                .partialRefund(partialRefund)
                .remainingRefundableAmount(remainingAmount)
                .build();
    }

    public static RefundResult failure(String errorMessage, String errorCode) {
        return RefundResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static RefundResult failure(String errorMessage, String errorCode,
            String gatewayResponseCode, String gatewayMessage) {
        return RefundResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .gatewayResponseCode(gatewayResponseCode)
                .gatewayMessage(gatewayMessage)
                .build();
    }
}