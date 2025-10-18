package com.paymenthub.payment_service.application.port.out;

import lombok.Value;
import lombok.Builder;
import java.time.LocalDateTime;

@Value
@Builder
public class VoidResult {
    boolean success;
    String gatewayTransactionId;
    LocalDateTime voidedAt;
    String errorMessage;
    String errorCode;
    String gatewayResponseCode;
    String gatewayMessage;

    // Gateway-specific data
    java.util.Map<String, Object> gatewayMetadata;

    public static VoidResult success(String gatewayTransactionId) {
        return VoidResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .voidedAt(LocalDateTime.now())
                .build();
    }

    public static VoidResult success(String gatewayTransactionId, java.util.Map<String, Object> metadata) {
        return VoidResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .voidedAt(LocalDateTime.now())
                .gatewayMetadata(metadata)
                .build();
    }

    public static VoidResult failure(String errorMessage, String errorCode) {
        return VoidResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static VoidResult failure(String errorMessage, String errorCode,
            String gatewayResponseCode, String gatewayMessage) {
        return VoidResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .gatewayResponseCode(gatewayResponseCode)
                .gatewayMessage(gatewayMessage)
                .build();
    }
}