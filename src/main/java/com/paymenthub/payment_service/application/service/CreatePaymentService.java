package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.result.PaymentResult;
import com.paymenthub.payment_service.application.port.in.usecase.CreatePaymentUseCase;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final EventBus eventPublisher;

    @Override
    @Transactional
    public PaymentResult createPayment(CreatePaymentCommand command) {

        List<Payment> existingPayments = paymentRepository.findAllByInvoiceId(command.invoiceId());

        if (existingPayments.size() > 10) {
            throw new TooManyPaymentAttemptsException(
                    "Invoice has exceeded maximum payment attempts. Please contact support.");
        }

        boolean hasActivePayment = existingPayments.stream()
                .anyMatch(payment -> payment.getStatus().isActivePayment());

        if (hasActivePayment) {
            throw new DuplicatePaymentException(
                    String.format("An active payment already exists for invoice: %s. " +
                            "Previous payment must be in a terminal state (FAILED, VOIDED, or REFUNDED) " +
                            "before creating a new payment.", command.invoiceId()));
        }

        InvoiceId invoiceId = new InvoiceId(command.invoiceId());
        Money amount = new Money(command.amount(), command.currency());

        Payment payment = Payment.createPendingPayment(invoiceId, amount);

        Payment savedPayment = paymentRepository.save(payment);

        if (!savedPayment.getDomainEvents().isEmpty()) {
            eventPublisher.publish(savedPayment.getDomainEvents());
            savedPayment.clearDomainEvents();
        }

        return PaymentResult.from(
                savedPayment.getId(),
                savedPayment.getInvoiceId().getValue(),
                savedPayment.getAuthorizedAmount().getAmount(),
                savedPayment.getAuthorizedAmount().getCurrencyCode(),
                savedPayment.getStatus(),
                savedPayment.getCreatedAt());
    }
}
