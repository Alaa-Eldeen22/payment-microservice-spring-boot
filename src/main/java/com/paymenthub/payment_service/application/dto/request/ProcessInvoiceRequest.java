package com.paymenthub.payment_service.application.dto.request;

import lombok.Value;
import lombok.Builder;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Value
@Builder
public class ProcessInvoiceRequest {
    @NotBlank(message = "Invoice ID is required")
    String invoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;

    @NotBlank(message = "Currency is required")
    String currency;

    String customerId;
    String paymentMethodId;
    String description;

    // Auto-authorize flag
    Boolean autoAuthorize;
}