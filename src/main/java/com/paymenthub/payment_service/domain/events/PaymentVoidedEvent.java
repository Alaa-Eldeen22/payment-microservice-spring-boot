package com.paymenthub.payment_service.domain.events;

import lombok.Getter;

@Getter
@EventType("payment.voided")
public class PaymentVoidedEvent extends PaymentEvent {

    private final String invoiceId;

    public PaymentVoidedEvent(String paymentId, String invoiceId) {
        super(paymentId);
        this.invoiceId = invoiceId;
    }
}
