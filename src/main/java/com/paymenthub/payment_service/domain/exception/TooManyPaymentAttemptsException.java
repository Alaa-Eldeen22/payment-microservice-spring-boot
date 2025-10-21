package com.paymenthub.payment_service.domain.exception;

public class TooManyPaymentAttemptsException extends PaymentDomainException {

    public TooManyPaymentAttemptsException(String message) {
        super(message);
    }
}
