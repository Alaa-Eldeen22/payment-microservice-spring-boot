package com.paymenthub.payment_service.application.dto.response;

import lombok.Value;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class CaptureResponse {
    boolean success;
    String paymentId;
    BigDecimal capturedAmount;
    BigDecimal totalCapturedAmount;
    BigDecimal remainingAmount;
    String currency;
    String status;
    LocalDateTime capturedAt;
    String errorMessage;
    String errorCode;

    public static CaptureResponse success(String paymentId, BigDecimal capturedAmount,
            BigDecimal totalCaptured, BigDecimal remaining,
            String currency, String status, LocalDateTime capturedAt) {
        return CaptureResponse.builder()
                .success(true)
                .paymentId(paymentId)
                .capturedAmount(capturedAmount)
                .totalCapturedAmount(totalCaptured)
                .remainingAmount(remaining)
                .currency(currency)
                .status(status)
                .capturedAt(capturedAt)
                .build();
    }

    public static CaptureResponse failure(String paymentId, String errorMessage, String errorCode) {
        return CaptureResponse.builder()
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
// public class CaptureResponse {
// boolean success;
// String paymentId;
// BigDecimal capturedAmount;
// BigDecimal totalCapturedAmount;
// String currency;
// LocalDateTime capturedAt;
// String errorMessage;
// }
