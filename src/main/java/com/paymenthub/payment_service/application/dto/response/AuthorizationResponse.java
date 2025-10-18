package com.paymenthub.payment_service.application.dto.response;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class AuthorizationResponse {
    boolean success;
    String paymentId;
    BigDecimal authorizedAmount;
    String currency;
    LocalDateTime authorizedAt;
    LocalDateTime expiresAt;
    String gatewayTransactionId;
    String errorMessage;
    String errorCode;

    public static AuthorizationResponse success(String paymentId, BigDecimal amount,
            String currency, LocalDateTime authorizedAt,
            LocalDateTime expiresAt, String gatewayTransactionId) {
        return AuthorizationResponse.builder()
                .success(true)
                .paymentId(paymentId)
                .authorizedAmount(amount)
                .currency(currency)
                .authorizedAt(authorizedAt)
                .expiresAt(expiresAt)
                .gatewayTransactionId(gatewayTransactionId)
                .build();
    }

    public static AuthorizationResponse failure(String paymentId, String errorMessage, String errorCode) {
        return AuthorizationResponse.builder()
                .success(false)
                .paymentId(paymentId)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }
}
// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import lombok.Builder;
// import lombok.Value;

// @Value
// @Builder
// public class AuthorizationResponse {
// boolean success;
// String paymentId;
// BigDecimal authorizedAmount;
// String currency;
// LocalDateTime authorizedAt;
// LocalDateTime expiresAt;
// String errorMessage;
// }
