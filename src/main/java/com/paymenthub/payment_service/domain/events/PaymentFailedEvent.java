package com.paymenthub.payment_service.domain.events;
import lombok.Getter;

@Getter
@EventType("payment.failed")
public class PaymentFailedEvent extends PaymentEvent {
    private final String reason;
    private final String invoiceId;

    public PaymentFailedEvent(String paymentId, String invoiceId, String reason) {
        super(paymentId);
        this.reason = reason;
        this.invoiceId = invoiceId;
    }
}
