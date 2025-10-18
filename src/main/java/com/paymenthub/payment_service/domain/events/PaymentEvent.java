package com.paymenthub.payment_service.domain.events;

public abstract class PaymentEvent extends DomainEvent {
    private final String paymentId;

    public PaymentEvent(String paymentId) {
        this.paymentId = paymentId;
    }

}
