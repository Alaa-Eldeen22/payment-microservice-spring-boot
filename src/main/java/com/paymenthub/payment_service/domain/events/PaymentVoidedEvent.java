package com.paymenthub.payment_service.domain.events;

@EventType("payment.voided")
public class PaymentVoidedEvent extends PaymentEvent {

    public PaymentVoidedEvent(String paymentId) {
        super(paymentId);
    }
}
