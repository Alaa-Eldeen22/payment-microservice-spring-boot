package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.mapper;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;

import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toDomainEntity(PaymentEntity jpa) {
        Payment payment = Payment.createPendingPayment(
                new InvoiceId(jpa.getInvoiceId()),
                new Money(jpa.getAuthorizedAmount(), jpa.getCurrency()));

        // Use reflection to set private fields that can't be modified through public
        // API
        try {
            var idField = Payment.class.getDeclaredField("id");
            var statusField = Payment.class.getDeclaredField("status");
            var stripePaymentIntentIdField = Payment.class.getDeclaredField("stripePaymentIntentId");
            var createdAtField = Payment.class.getDeclaredField("createdAt");
            var authorizedAtField = Payment.class.getDeclaredField("authorizedAt");
            var capturedAtField = Payment.class.getDeclaredField("capturedAt");
            var expiresAtField = Payment.class.getDeclaredField("expiresAt");
            var capturedAmountField = Payment.class.getDeclaredField("capturedAmount");

            idField.setAccessible(true);
            statusField.setAccessible(true);
            stripePaymentIntentIdField.setAccessible(true);
            createdAtField.setAccessible(true);
            authorizedAtField.setAccessible(true);
            capturedAtField.setAccessible(true);
            expiresAtField.setAccessible(true);
            capturedAmountField.setAccessible(true);

            idField.set(payment, jpa.getId());
            statusField.set(payment, jpa.getStatus());
            stripePaymentIntentIdField.set(payment, jpa.getStripePaymentIntentId());
            createdAtField.set(payment, jpa.getCreatedAt());
            authorizedAtField.set(payment, jpa.getAuthorizedAt());
            capturedAtField.set(payment, jpa.getCapturedAt());
            expiresAtField.set(payment, jpa.getExpiresAt());
            capturedAmountField.set(payment, new Money(jpa.getCapturedAmount(), jpa.getCurrency()));

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error mapping PaymentJpaEntity to Payment", e);
        }

        return payment;
    }

    public PaymentEntity toJpaEntity(Payment payment) {
        PaymentEntity jpa = new PaymentEntity();
        jpa.setId(payment.getId());
        jpa.setInvoiceId(payment.getInvoiceId().getValue());
        jpa.setAuthorizedAmount(payment.getAuthorizedAmount().getAmount());
        jpa.setCapturedAmount(payment.getCapturedAmount().getAmount());
        jpa.setCurrency(payment.getAuthorizedAmount().getCurrencyCode());
        jpa.setStatus(payment.getStatus());
        jpa.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        jpa.setCreatedAt(payment.getCreatedAt());
        jpa.setAuthorizedAt(payment.getAuthorizedAt());
        jpa.setCapturedAt(payment.getCapturedAt());
        jpa.setExpiresAt(payment.getExpiresAt());
        return jpa;
    }
}