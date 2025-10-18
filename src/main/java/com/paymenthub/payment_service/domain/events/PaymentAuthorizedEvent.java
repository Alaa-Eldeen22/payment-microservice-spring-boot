package com.paymenthub.payment_service.domain.events;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class PaymentAuthorizedEvent extends DomainEvent {
    private final String paymentId;
    private final String invoiceId;
    private final BigDecimal amount;
    private final String currency;

    public PaymentAuthorizedEvent(String paymentId, String invoiceId, BigDecimal amount, String currency) {
        super();
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.currency = currency;
    }
}
