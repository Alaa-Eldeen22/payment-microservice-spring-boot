package com.paymenthub.payment_service.application.port.out;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class CaptureResult {
    boolean success;
    String gatewayTransactionId;
    BigDecimal capturedAmount;
    String currency;
    LocalDateTime capturedAt;
    String errorMessage;
    String errorCode;
    String gatewayResponseCode;
    String gatewayMessage;

    // For partial captures
    boolean finalCapture;
    BigDecimal remainingAmount;

    // Gateway-specific data
    java.util.Map<String, Object> gatewayMetadata;

    public static CaptureResult success(String gatewayTransactionId, BigDecimal capturedAmount, String currency) {
        return CaptureResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .capturedAmount(capturedAmount)
                .currency(currency)
                .capturedAt(LocalDateTime.now())
                .build();
    }

    public static CaptureResult success(String gatewayTransactionId, BigDecimal capturedAmount,
            String currency, boolean finalCapture, BigDecimal remainingAmount) {
        return CaptureResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .capturedAmount(capturedAmount)
                .currency(currency)
                .capturedAt(LocalDateTime.now())
                .finalCapture(finalCapture)
                .remainingAmount(remainingAmount)
                .build();
    }

    public static CaptureResult failure(String errorMessage, String errorCode) {
        return CaptureResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static CaptureResult failure(String errorMessage, String errorCode,
            String gatewayResponseCode, String gatewayMessage) {
        return CaptureResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .gatewayResponseCode(gatewayResponseCode)
                .gatewayMessage(gatewayMessage)
                .build();
    }
}