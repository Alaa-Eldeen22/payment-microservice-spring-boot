package com.paymenthub.payment_service.application.port.out;

import java.math.BigDecimal;

import com.paymenthub.payment_service.application.exception.PaymentGatewayException;

/**
 * Port for interacting with payment gateway (Stripe, PayPal, etc.)
 */
public interface PaymentGateway {
    /**
     * Authorize payment with customer's saved payment method
     * 
     * @return Gateway reference ID (e.g., Stripe Payment Intent ID)
     * @throws PaymentGatewayException if authorization fails
     */
    String authorize(
            String paymentId,
            String customerId,
            String paymentMethodId,
            BigDecimal amount,
            String currency) throws PaymentGatewayException;

    /**
     * Capture authorized payment
     */
    void capture(String gatewayReferenceId, BigDecimal amount) throws PaymentGatewayException;

    /**
     * Void authorization
     */
    void voidAuthorization(String gatewayReferenceId) throws PaymentGatewayException;
}