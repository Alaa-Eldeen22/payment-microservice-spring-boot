package com.paymenthub.payment_service.domain.events;

public class PaymentVoidedEvent extends DomainEvent {
    private final String paymentId;
    // private final String reason;

    public PaymentVoidedEvent(String paymentId) {
        super();
        this.paymentId = paymentId;
        // this.reason = reason;
    }
}
