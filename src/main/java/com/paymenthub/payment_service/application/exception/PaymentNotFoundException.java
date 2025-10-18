package com.paymenthub.payment_service.application.exception;

public class PaymentNotFoundException extends RuntimeException {
    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super(String.format("Payment not found with ID: %s", paymentId));
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}