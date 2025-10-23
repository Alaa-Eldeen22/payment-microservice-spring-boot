package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.mapper;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import com.paymenthub.payment_service.domain.valueobject.PaymentMethodId;
import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;

import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toDomainEntity(PaymentEntity entity) {
        Money authorized = new Money(entity.getAuthorizedAmount(), entity.getCurrency());
        Money captured = entity.getCapturedAmount() != null
                ? new Money(entity.getCapturedAmount(), entity.getCurrency())
                : null;

        return Payment.builder()
                .id(entity.getId())
                .invoiceId(new InvoiceId(entity.getInvoiceId()))
                .authorizedAmount(authorized)
                .capturedAmount(captured)
                .status(entity.getStatus())
                .paymentGatewayReferenceId(entity.getPaymentGatewayReferenceId())
                .createdAt(entity.getCreatedAt())
                .authorizedAt(entity.getAuthorizedAt())
                .capturedAt(entity.getCapturedAt())
                .expiresAt(entity.getExpiresAt())
                .paymentMethodId(new PaymentMethodId(entity.getPaymentMethodId()))
                .build();
    }

    public PaymentEntity toJpaEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId());
        entity.setInvoiceId(payment.getInvoiceId().getValue());
        entity.setAuthorizedAmount(payment.getAuthorizedAmount().getAmount());
        entity.setCapturedAmount(payment.getCapturedAmount() != null ? payment.getCapturedAmount().getAmount() : null);
        entity.setCurrency(payment.getAuthorizedAmount().getCurrencyCode());
        entity.setStatus(payment.getStatus());
        entity.setPaymentGatewayReferenceId(payment.getPaymentGatewayReferenceId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setAuthorizedAt(payment.getAuthorizedAt());
        entity.setCapturedAt(payment.getCapturedAt());
        entity.setExpiresAt(payment.getExpiresAt());
        entity.setPaymentMethodId(payment.getPaymentMethodId().getValue());
        return entity;
    }
}