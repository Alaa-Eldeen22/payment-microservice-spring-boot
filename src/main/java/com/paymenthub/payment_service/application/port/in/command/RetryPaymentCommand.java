package com.paymenthub.payment_service.application.port.in.command;

import jakarta.validation.constraints.NotBlank;

/**
 * Command to retry payment for an invoice
 * Used when user manually retries after failure
 */
public record RetryPaymentCommand(
        @NotBlank String invoiceId,
        @NotBlank String customerId,
        @NotBlank String paymentMethodId) {
}