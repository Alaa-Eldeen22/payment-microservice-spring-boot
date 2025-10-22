package com.paymenthub.payment_service.application.exception;

public class PaymentGatewayException extends RuntimeException {

    private final String errorCode;
    private final String gatewayMessage;

    public PaymentGatewayException(String message, String errorCode, String gatewayMessage) {
        super(message);
        this.errorCode = errorCode;
        this.gatewayMessage = gatewayMessage;
    }

    public PaymentGatewayException(String message, String errorCode, String gatewayMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.gatewayMessage = gatewayMessage;
    }

    public PaymentGatewayException(String message) {
        this(message, null, null);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getGatewayMessage() {
        return gatewayMessage;
    }
}