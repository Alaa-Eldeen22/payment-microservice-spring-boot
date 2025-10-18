package com.paymenthub.payment_service.domain.events;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
@EventType("payment.authorized")
public class PaymentAuthorizedEvent extends PaymentEvent {
    private final String invoiceId;
    private final BigDecimal amount;
    private final String currency;

    public PaymentAuthorizedEvent(String paymentId, String invoiceId, BigDecimal amount, String currency) {
        super(paymentId);
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.currency = currency;
    }
}
