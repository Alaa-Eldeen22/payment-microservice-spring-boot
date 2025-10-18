package com.paymenthub.payment_service.domain.events;

public class PaymentFailedEvent extends DomainEvent {
    private final String paymentId;
    private final String reason;
    
    public PaymentFailedEvent(String paymentId, String reason) {
        super();
        this.paymentId = paymentId;
        this.reason = reason;
    }
}
