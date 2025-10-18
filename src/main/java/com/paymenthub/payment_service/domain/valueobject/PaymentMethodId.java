package com.paymenthub.payment_service.domain.valueobject;

public class PaymentMethodId {

    String value;

    public PaymentMethodId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment Method ID cannot be null or empty");
        }
        this.value = value.trim();
    }
}
