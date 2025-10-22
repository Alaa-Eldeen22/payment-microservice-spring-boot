package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic payment flow service
 * Creates payment and immediately attempts authorization with gateway
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CreateAndAuthorizePaymentService implements CreateAndAuthorizePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventBus eventBus;

    @Override
    @Transactional
    public PaymentResult execute(CreateAndAuthorizePaymentCommand command) {
        log.info("Creating and authorizing payment for invoice: {}", command.invoiceId());

        // 1. Check for active payment (prevent duplicates)
        InvoiceId invoiceId = new InvoiceId(command.invoiceId());
        List<Payment> existingPayments = paymentRepository.findAllByInvoiceId(new InvoiceId(command.invoiceId()));

        if (existingPayments.size() > 10) {
            throw new TooManyPaymentAttemptsException(
                    "Invoice has exceeded maximum payment attempts. Please contact support.");
        }

        // 2. Create Value Objects
        Money amount = new Money(command.amount(), command.currency());

        // 3. Create Payment (PENDING)
        Payment payment = Payment.createPendingPayment(invoiceId, amount);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created: {} - attempting authorization", savedPayment.getId());

        // 4. Call Payment Gateway to authorize
        try {
            String gatewayReferenceId = paymentGateway.authorize(
                    savedPayment.getId(),
                    command.customerId(),
                    command.paymentMethodId(),
                    command.amount(),
                    command.currency());

            // 5. Authorization succeeded
            savedPayment.authorize(gatewayReferenceId);
            paymentRepository.save(savedPayment);

            log.info("Payment authorized: {} with gateway reference: {}",
                    savedPayment.getId(), gatewayReferenceId);

        } catch (PaymentGatewayException e) {
            // 6. Authorization failed (card declined, insufficient funds, etc.)
            log.error("Payment authorization failed for payment: {} - Reason: {}",
                    savedPayment.getId(), e.getMessage());

            savedPayment.markAsFailed(e.getMessage());
            paymentRepository.save(savedPayment);
        }

        // 7. Publish events (either AUTHORIZED or FAILED)
        if (!savedPayment.getDomainEvents().isEmpty()) {
            eventBus.publish(savedPayment.getDomainEvents());
            savedPayment.clearDomainEvents();
        }

        return PaymentResult.fromDomain(savedPayment);
    }
}