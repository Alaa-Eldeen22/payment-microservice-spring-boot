package com.paymenthub.payment_service.application.port.in.command;

public record VoidPaymentCommand(
        String paymentId) {
}