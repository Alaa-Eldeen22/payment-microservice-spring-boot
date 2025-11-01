package com.paymenthub.payment_service.domain.exception;

public class PaymentNotFoundException extends PaymentDomainException {
    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super(String.format("Payment not found with ID: %s", paymentId));
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}