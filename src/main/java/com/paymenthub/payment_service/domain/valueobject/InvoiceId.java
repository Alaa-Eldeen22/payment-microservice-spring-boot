package com.paymenthub.payment_service.domain.valueobject;

import lombok.Value;

@Value
public class InvoiceId {
    String value;

    public InvoiceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        this.value = value.trim();
    }
}