package com.paymenthub.payment_service.domain.events;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@EventType("payment.captured")
public class PaymentCapturedEvent extends PaymentEvent {
    private final String invoiceId;
    private final LocalDateTime capturedAt;

    public PaymentCapturedEvent(String paymentId, String invoiceId, LocalDateTime capturedAt) {
        super(paymentId);
        this.invoiceId = invoiceId;
        this.capturedAt = capturedAt;
    }
}
