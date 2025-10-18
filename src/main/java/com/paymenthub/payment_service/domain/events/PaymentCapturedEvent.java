package com.paymenthub.payment_service.domain.events;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
@EventType("payment. captured")
public class PaymentCapturedEvent extends PaymentEvent {
    private final BigDecimal capturedAmount;
    private final BigDecimal totalCapturedAmount;

    public PaymentCapturedEvent(String paymentId, BigDecimal capturedAmount, BigDecimal totalCapturedAmount) {
        super(paymentId);
        this.capturedAmount = capturedAmount;
        this.totalCapturedAmount = totalCapturedAmount;
    }
}
