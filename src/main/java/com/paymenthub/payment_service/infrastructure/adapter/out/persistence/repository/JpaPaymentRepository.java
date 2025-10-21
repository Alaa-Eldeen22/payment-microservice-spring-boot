package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;

interface JpaPaymentRepository extends JpaRepository<PaymentEntity, String> {
    boolean existsByInvoiceId(String invoiceId);

    PaymentEntity findByInvoiceId(String invoiceId);

}