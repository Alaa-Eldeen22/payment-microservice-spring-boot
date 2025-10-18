package com.paymenthub.payment_service.application.dto.request;

import lombok.Value;
import lombok.Builder;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Value
@Builder
public class CapturePaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;

    @NotNull(message = "Capture amount is required")
    @DecimalMin(value = "0.01", message = "Capture amount must be greater than 0")
    BigDecimal amount;

    String description;
    String statementDescriptor;

    // For partial captures
    Boolean finalCapture;
}