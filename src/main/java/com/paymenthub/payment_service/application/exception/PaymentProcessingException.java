package com.paymenthub.payment_service.application.exception;

public class PaymentProcessingException extends RuntimeException {
    private final String paymentId;

    public PaymentProcessingException(String paymentId, String message) {
        super(String.format("Payment processing failed for payment %s: %s", paymentId, message));
        this.paymentId = paymentId;
    }

    public PaymentProcessingException(String paymentId, String message, Throwable cause) {
        super(String.format("Payment processing failed for payment %s: %s", paymentId, message), cause);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}