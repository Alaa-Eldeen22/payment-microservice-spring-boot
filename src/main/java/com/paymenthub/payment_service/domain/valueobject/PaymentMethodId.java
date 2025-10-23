package com.paymenthub.payment_service.domain.valueobject;

import java.util.Objects;

public class PaymentMethodId {
    private final String value;

    public PaymentMethodId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentMethodId cannot be null or empty");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PaymentMethodId))
            return false;
        PaymentMethodId other = (PaymentMethodId) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

}