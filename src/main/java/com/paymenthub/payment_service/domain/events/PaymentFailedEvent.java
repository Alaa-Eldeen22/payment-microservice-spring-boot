package com.paymenthub.payment_service.domain.events;

@EventType("payment.failed")
public class PaymentFailedEvent extends PaymentEvent {
    private final String reason;

    public PaymentFailedEvent(String paymentId, String reason) {
        super(paymentId);
        this.reason = reason;
    }
}
