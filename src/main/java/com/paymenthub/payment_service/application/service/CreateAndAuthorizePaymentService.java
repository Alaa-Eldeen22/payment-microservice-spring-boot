package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;
import com.paymenthub.payment_service.domain.valueobject.PaymentMethodId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateAndAuthorizePaymentService implements CreateAndAuthorizePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventBus eventBus;

    @Override
    @Transactional
    public PaymentResult createAndAuthorize(CreateAndAuthorizePaymentCommand command) {
        // TODO: Use an injected logger

        log.info("Creating and authorizing payment for invoice: {}", command.invoiceId());

        InvoiceId invoiceId = new InvoiceId(command.invoiceId());

        List<Payment> existingPayments = paymentRepository.findAllByInvoiceId(new InvoiceId(command.invoiceId()));

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

        Money amount = new Money(command.amount(), command.currency());

        PaymentMethodId paymentMethodId = new PaymentMethodId(command.paymentMethodId());

        Payment payment = Payment.createPendingPayment(invoiceId, paymentMethodId, amount);

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created: {} - attempting authorization", savedPayment.getId());

        try {
            String gatewayReferenceId = paymentGateway.authorize(
                    savedPayment.getId(),
                    command.customerId(),
                    command.paymentMethodId(),
                    command.amount(),
                    command.currency());

            savedPayment.authorize(gatewayReferenceId);
            paymentRepository.save(savedPayment);

            log.info("Payment authorized: {} with gateway reference: {}",
                    savedPayment.getId(), gatewayReferenceId);

        } catch (PaymentGatewayException e) {
            log.error("Payment authorization failed for payment: {} - Reason: {}",
                    savedPayment.getId(), e.getMessage());

            savedPayment.markAsFailed(e.getMessage());
            paymentRepository.save(savedPayment);
        }

        if (!savedPayment.getDomainEvents().isEmpty()) {
            eventBus.publish(savedPayment.getDomainEvents());
            savedPayment.clearDomainEvents();
        }

        return PaymentResult.fromDomain(savedPayment);
    }
}