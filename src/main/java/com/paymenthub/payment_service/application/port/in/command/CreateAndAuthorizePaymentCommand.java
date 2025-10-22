package com.paymenthub.payment_service.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Command to create payment and immediately authorize it
 * Used for automatic payments triggered by invoice creation
 */
public record CreateAndAuthorizePaymentCommand(
        @NotBlank String invoiceId,
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethodId) {
}