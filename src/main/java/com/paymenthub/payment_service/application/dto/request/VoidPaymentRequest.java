package com.paymenthub.payment_service.application.dto.request;

import lombok.Value;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Value
@Builder
public class VoidPaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;

    String reason;
    String description;
}