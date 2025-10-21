package com.paymenthub.payment_service.domain.repository;

import java.util.List;
import java.util.Optional;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    boolean existsByInvoiceId(InvoiceId invoiceId);

    Optional<Payment> findByInvoiceId(InvoiceId invoiceId);

    List<Payment> findAllByInvoiceId(InvoiceId invoiceId);

}
