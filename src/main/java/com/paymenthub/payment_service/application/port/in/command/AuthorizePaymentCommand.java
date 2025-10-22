package com.paymenthub.payment_service.application.port.in.command;

import jakarta.validation.constraints.NotBlank;

public record AuthorizePaymentCommand(
    @NotBlank(message = "Payment ID is required")
    String paymentId,
    
    @NotBlank(message = "Payment gateway reference ID is required")
    String paymentGatewayReferenceId
) { }