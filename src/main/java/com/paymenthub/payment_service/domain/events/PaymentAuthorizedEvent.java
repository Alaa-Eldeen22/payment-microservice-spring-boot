package com.paymenthub.payment_service.domain.events;

import lombok.Getter;

@Getter
@EventType("payment.authorized")
public class PaymentAuthorizedEvent extends PaymentEvent {
    private final String invoiceId;

    public PaymentAuthorizedEvent(String paymentId, String invoiceId) {
        super(paymentId);
        this.invoiceId = invoiceId;
    }
}
