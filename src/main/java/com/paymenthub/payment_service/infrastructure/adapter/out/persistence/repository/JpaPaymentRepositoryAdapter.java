package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.repository;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JpaPaymentRepositoryAdapter implements PaymentRepository {
    private final JpaPaymentRepository jpaPaymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity paymentJpaEntity = paymentMapper.toJpaEntity(payment);
        PaymentEntity savedEntity = jpaPaymentRepository.save(paymentJpaEntity);
        return paymentMapper.toDomainEntity(savedEntity);
    }

    @Override
    public Optional<Payment> findById(String id) {
        return jpaPaymentRepository.findById(id)
                .map(paymentMapper::toDomainEntity);
    }

    @Override
    public boolean existsByInvoiceId(String invoiceId) {
        return jpaPaymentRepository.existsByInvoiceId(invoiceId);
    }

    @Override
    public Optional<Payment> findByInvoiceId(InvoiceId invoiceId) {
        return Optional.ofNullable(jpaPaymentRepository.findByInvoiceId(invoiceId.getValue()))
                .map(paymentMapper::toDomainEntity);
    }

    @Override
    public List<Payment> findAllByInvoiceId(String invoiceId) {
        return jpaPaymentRepository.findAllByInvoiceId(invoiceId)
                .stream()
                .map(paymentMapper::toDomainEntity)
                .toList();
    }

}