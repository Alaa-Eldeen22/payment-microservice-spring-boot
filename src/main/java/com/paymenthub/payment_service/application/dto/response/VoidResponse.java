package com.paymenthub.payment_service.application.dto.response;

import lombok.Value;
import lombok.Builder;
import java.time.LocalDateTime;

@Value
@Builder
public class VoidResponse {
    boolean success;
    String paymentId;
    LocalDateTime voidedAt;
    String errorMessage;
    String errorCode;

    public static VoidResponse success(String paymentId, LocalDateTime voidedAt) {
        return VoidResponse.builder()
                .success(true)
                .paymentId(paymentId)
                .voidedAt(voidedAt)
                .build();
    }

    public static VoidResponse failure(String paymentId, String errorMessage, String errorCode) {
        return VoidResponse.builder()
                .success(false)
                .paymentId(paymentId)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }
}