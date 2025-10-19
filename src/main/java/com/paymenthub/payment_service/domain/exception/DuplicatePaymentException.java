package com.paymenthub.payment_service.domain.exception;

public class DuplicatePaymentException extends PaymentDomainException {
    public DuplicatePaymentException(String message) {
        super(message);
    }

}
