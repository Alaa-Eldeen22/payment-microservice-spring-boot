package com.paymenthub.payment_service.infrastructure.adapter.out.gateway;

import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public String authorize(
            String paymentId,
            String customerId,
            String paymentMethodId,
            BigDecimal amount,
            String currency) throws PaymentGatewayException {

        try {
            log.info("Calling Stripe to authorize payment: {}", paymentId);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(new BigDecimal("100")).longValue())
                    .setCurrency(currency.toLowerCase())
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true) // Immediately authorize
                    .setOffSession(true) // Charge without customer present
                    .putMetadata("payment_id", paymentId)
                    .build();

            // PaymentIntent paymentIntent = PaymentIntent.create(params);

            // if (paymentIntent.getStatus().equals("succeeded")) {
            // log.info("Stripe authorization succeeded: {}", paymentIntent.getId());
            // return paymentIntent.getId();
            // } else {
            // throw new PaymentGatewayException(
            // "Payment authorization failed with status: " + paymentIntent.getStatus());
            // }
            // Simulate successful authorization for this example
            String simulatedPaymentIntentId = "pi_" + paymentId;
            log.info("Stripe authorization succeeded: {}", simulatedPaymentIntentId);
            return simulatedPaymentIntentId;
            // TODO: Uncomment above and remove simulation when integrating with real Stripe
            // API and catch StripeException
        } catch (RuntimeException e) {
            log.error("Stripe API error: {}", e.getMessage());
            throw new PaymentGatewayException("Card declined: " + e.getMessage(), e);
        }
    }

    @Override
    public void capture(String gatewayReferenceId, BigDecimal amount) throws PaymentGatewayException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayReferenceId);
            paymentIntent.capture();
            log.info("Payment captured: {}", gatewayReferenceId);
        } catch (StripeException e) {
            throw new PaymentGatewayException("Capture failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void voidAuthorization(String gatewayReferenceId) throws PaymentGatewayException {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(gatewayReferenceId);
            paymentIntent.cancel();
            log.info("Authorization voided: {}", gatewayReferenceId);
        } catch (StripeException e) {
            throw new PaymentGatewayException("Void failed: " + e.getMessage(), e);
        }
    }
}