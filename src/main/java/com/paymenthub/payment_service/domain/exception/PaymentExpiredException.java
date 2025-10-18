package com.paymenthub.payment_service.domain.exception;
public class PaymentExpiredException extends PaymentDomainException {
    public PaymentExpiredException(String message) {
        super(message);
    }
}