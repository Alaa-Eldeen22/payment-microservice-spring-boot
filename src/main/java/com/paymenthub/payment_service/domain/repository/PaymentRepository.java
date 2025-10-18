package com.paymenthub.payment_service.domain.repository;

import java.util.Optional;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    Optional<Payment> findByInvoiceId(InvoiceId invoiceId);

    List<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Payment> findExpiringAuthorizations(LocalDateTime before);

    void delete(Payment payment);
}
