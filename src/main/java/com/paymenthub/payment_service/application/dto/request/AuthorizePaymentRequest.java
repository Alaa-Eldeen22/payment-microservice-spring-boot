package com.paymenthub.payment_service.application.dto.request;

import lombok.Value;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Value
@Builder
public class AuthorizePaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;

    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId;

    String customerId;
    String description;

    // Additional metadata
    java.util.Map<String, String> metadata;
}