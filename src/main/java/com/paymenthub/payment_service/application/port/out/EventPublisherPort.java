package com.paymenthub.payment_service.application.port.out;

import com.paymenthub.payment_service.domain.events.*;

public interface EventPublisherPort {
    void publishPaymentAuthorized(PaymentAuthorizedEvent event);

    void publishPaymentCaptured(PaymentCapturedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishPaymentVoided(PaymentVoidedEvent event);
}