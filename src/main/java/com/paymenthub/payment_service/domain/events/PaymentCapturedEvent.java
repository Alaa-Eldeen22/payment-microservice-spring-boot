package com.paymenthub.payment_service.domain.events;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class PaymentCapturedEvent extends DomainEvent {
    private final String paymentId;
    private final BigDecimal capturedAmount;
    private final BigDecimal totalCapturedAmount;

    public PaymentCapturedEvent(String paymentId, BigDecimal capturedAmount, BigDecimal totalCapturedAmount) {
        super();
        this.paymentId = paymentId;
        this.capturedAmount = capturedAmount;
        this.totalCapturedAmount = totalCapturedAmount;
    }
}
