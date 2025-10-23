package com.paymenthub.payment_service.domain.valueobject;

import java.util.Objects;

public final class InvoiceId {

    private final String value;

    public InvoiceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof InvoiceId))
            return false;
        InvoiceId other = (InvoiceId) o;
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
