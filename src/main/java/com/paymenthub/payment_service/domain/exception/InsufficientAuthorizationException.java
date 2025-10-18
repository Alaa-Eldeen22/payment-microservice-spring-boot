package com.paymenthub.payment_service.domain.exception;

public class InsufficientAuthorizationException extends PaymentDomainException {
    public InsufficientAuthorizationException(String message) {
        super(message);
    }
}