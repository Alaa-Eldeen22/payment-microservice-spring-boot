package com.paymenthub.payment_service.application.exception;

public class PaymentCaptureFailedException extends RuntimeException {
    private final String paymentId;
    private final String errorCode;

    public PaymentCaptureFailedException(String paymentId, String message, String errorCode) {
        super(String.format("Payment capture failed for payment %s: %s", paymentId, message));
        this.paymentId = paymentId;
        this.errorCode = errorCode;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}