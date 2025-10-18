package com.paymenthub.payment_service.application.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreatePaymentRequest {
    @NotBlank(message = "Invoice ID is required")
    String invoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency;

    String description;
    String customerId;
}