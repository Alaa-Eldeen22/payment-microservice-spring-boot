package com.paymenthub.payment_service.application.exception;

import java.util.List;

public class PaymentValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public PaymentValidationException(List<String> validationErrors) {
        super("Payment validation failed: " + String.join(", ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}