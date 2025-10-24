package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.enums.PaymentStatus;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import com.paymenthub.payment_service.domain.valueobject.PaymentMethodId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreatePendingPaymentService {

    private static final int MAX_PAYMENT_ATTEMPTS = 10;

    private final PaymentRepository paymentRepository;
    private final EventBus eventBus;

    @Transactional
    public Payment create(CreatePaymentCommand command) {
        log.info("Starting pending payment creation for invoice ID: {}", command.invoiceId());

        InvoiceId invoiceId = new InvoiceId(command.invoiceId());
        validatePaymentAttempts(invoiceId);
        ensureNoActivePayment(invoiceId);

        PaymentMethodId paymentMethodId = new PaymentMethodId(command.paymentMethodId());
        Money amount = new Money(command.amount(), command.currency());

        Payment payment = Payment.createPendingPayment(invoiceId, paymentMethodId, amount);
        Payment savedPayment = paymentRepository.save(payment);

        publishDomainEvents(savedPayment);

        log.info("Successfully created pending payment with ID: {}", savedPayment.getId());
        return savedPayment;
    }

    private void validatePaymentAttempts(InvoiceId invoiceId) {
        int attempts = paymentRepository.countByInvoiceId(invoiceId);
        if (attempts >= MAX_PAYMENT_ATTEMPTS) {
            throw new TooManyPaymentAttemptsException(
                    "Invoice has exceeded maximum payment attempts. Please contact support.");
        }
    }

    private void ensureNoActivePayment(InvoiceId invoiceId) {
        List<PaymentStatus> activeStatuses = PaymentStatus.getActiveStatuses();
        if (paymentRepository.existsByInvoiceIdAndStatusIn(invoiceId, activeStatuses)) {
            throw new DuplicatePaymentException(
                    String.format("An active payment already exists for invoice ID: %s", invoiceId.getValue()));
        }
    }

    private void publishDomainEvents(Payment payment) {
        if (!payment.getDomainEvents().isEmpty()) {
            eventBus.publish(payment.getDomainEvents());
            payment.clearDomainEvents();
        }
    }
}
