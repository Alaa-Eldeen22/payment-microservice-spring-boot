package com.paymenthub.payment_service.application.port.out;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;

@Value
@Builder
public class AuthorizationResult {
    boolean success;
    String gatewayTransactionId;
    BigDecimal authorizedAmount;
    String currency;
    String errorMessage;
    String errorCode;
    String gatewayResponseCode;
    String gatewayMessage;

    // Gateway-specific data
    java.util.Map<String, Object> gatewayMetadata;

    public static AuthorizationResult success(String gatewayTransactionId, BigDecimal authorizedAmount,
            String currency) {
        return AuthorizationResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .authorizedAmount(authorizedAmount)
                .currency(currency)
                .build();
    }

    public static AuthorizationResult success(String gatewayTransactionId, BigDecimal authorizedAmount,
            String currency, java.util.Map<String, Object> metadata) {
        return AuthorizationResult.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .authorizedAmount(authorizedAmount)
                .currency(currency)
                .gatewayMetadata(metadata)
                .build();
    }

    public static AuthorizationResult failure(String errorMessage, String errorCode) {
        return AuthorizationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static AuthorizationResult failure(String errorMessage, String errorCode,
            String gatewayResponseCode, String gatewayMessage) {
        return AuthorizationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .gatewayResponseCode(gatewayResponseCode)
                .gatewayMessage(gatewayMessage)
                .build();
    }
}