package com.paymenthub.payment_service.domain.exception;

public class IllegalPaymentStateException extends PaymentDomainException {
    public IllegalPaymentStateException(String message) {
        super(message);
    }
}