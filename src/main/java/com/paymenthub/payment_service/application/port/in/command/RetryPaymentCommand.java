package com.paymenthub.payment_service.application.port.in.command;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command to retry payment for an invoice
 * Used when user manually retries after failure
 */
public record RetryPaymentCommand(
        @NotBlank String invoiceId,
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethodId) {
}