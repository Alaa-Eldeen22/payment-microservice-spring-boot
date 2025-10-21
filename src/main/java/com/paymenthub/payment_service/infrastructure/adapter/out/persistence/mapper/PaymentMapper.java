package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.mapper;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;

import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toDomainEntity(PaymentEntity entity) {
        Money authorized = new Money(entity.getAuthorizedAmount(), entity.getCurrency());
        Money captured = entity.getCapturedAmount() != null
                ? new Money(entity.getCapturedAmount(), entity.getCurrency())
                : null;

        return Payment.reconstitute(
                entity.getId(),
                new InvoiceId(entity.getInvoiceId()),
                authorized,
                captured,
                entity.getStatus(),
                entity.getStripePaymentIntentId(),
                entity.getCreatedAt(),
                entity.getAuthorizedAt(),
                entity.getCapturedAt(),
                entity.getExpiresAt());
    }

    public PaymentEntity toJpaEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId());
        entity.setInvoiceId(payment.getInvoiceId().getValue());
        entity.setAuthorizedAmount(payment.getAuthorizedAmount().getAmount());
        entity.setCapturedAmount(payment.getCapturedAmount() != null ? payment.getCapturedAmount().getAmount() : null);
        entity.setCurrency(payment.getAuthorizedAmount().getCurrencyCode());
        entity.setStatus(payment.getStatus());
        entity.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setAuthorizedAt(payment.getAuthorizedAt());
        entity.setCapturedAt(payment.getCapturedAt());
        entity.setExpiresAt(payment.getExpiresAt());
        return entity;
    }
}