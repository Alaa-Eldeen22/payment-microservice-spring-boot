package com.paymenthub.payment_service.domain.events;

import lombok.Getter;

@Getter
public abstract class PaymentEvent extends DomainEvent {
    private final String paymentId;

    public PaymentEvent(String paymentId) {
        this.paymentId = paymentId;
    }

}
