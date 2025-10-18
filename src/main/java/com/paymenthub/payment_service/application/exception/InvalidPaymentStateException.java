package com.paymenthub.payment_service.application.exception;

public class InvalidPaymentStateException extends RuntimeException {
    private final String paymentId;
    private final String currentState;
    private final String requestedOperation;

    public InvalidPaymentStateException(String paymentId, String currentState, String requestedOperation) {
        super(String.format("Invalid operation '%s' for payment %s in state '%s'",
                requestedOperation, paymentId, currentState));
        this.paymentId = paymentId;
        this.currentState = currentState;
        this.requestedOperation = requestedOperation;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getRequestedOperation() {
        return requestedOperation;
    }
}