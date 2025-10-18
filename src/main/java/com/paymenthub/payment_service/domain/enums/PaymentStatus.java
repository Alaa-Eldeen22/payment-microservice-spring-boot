package com.paymenthub.payment_service.domain.enums;

public enum PaymentStatus {
    PENDING("Payment is pending authorization"),
    AUTHORIZED("Payment is authorized, awaiting capture"),
    PARTIALLY_CAPTURED("Payment is partially captured"),
    CAPTURED("Payment is fully captured"),
    FAILED("Payment authorization or capture failed"),
    VOIDED("Payment authorization was voided"),
    REFUNDED("Payment was refunded after capture"),
    PARTIALLY_REFUNDED("Payment was partially refunded");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canBeAuthorized() {
        return this == PENDING;
    }

    public boolean canBeCaptured() {
        return this == AUTHORIZED || this == PARTIALLY_CAPTURED;
    }

    public boolean canBeVoided() {
        return this == AUTHORIZED || this == PARTIALLY_CAPTURED;
    }

    public boolean canBeRefunded() {
        return this == CAPTURED || this == PARTIALLY_REFUNDED;
    }
}
