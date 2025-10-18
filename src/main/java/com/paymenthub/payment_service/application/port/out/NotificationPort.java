package com.paymenthub.payment_service.application.port.out;

public interface NotificationPort {
    void notifyPaymentAuthorized(String customerId, String paymentId, String invoiceId);

    void notifyPaymentCaptured(String customerId, String paymentId, String invoiceId);

    void notifyPaymentFailed(String customerId, String paymentId, String invoiceId, String reason);
}