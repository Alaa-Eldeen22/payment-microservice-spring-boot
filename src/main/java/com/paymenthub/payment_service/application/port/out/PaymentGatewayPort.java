package com.paymenthub.payment_service.application.port.out;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.Money;

public interface PaymentGatewayPort {
    AuthorizationResult authorizePayment(Payment payment, String paymentMethodId, String customerId);

    CaptureResult capturePayment(String gatewayTransactionId, Money amount);

    VoidResult voidAuthorization(String gatewayTransactionId);

    RefundResult refundPajyment(String gatewayTransactionId, Money amount);
}