package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.RetryPaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.RetryPaymentUseCase;
import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.application.port.out.EventBus;
// import com.paymenthub.payment_service.application.port.out.InvoiceService;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual payment retry service
 * Allows user to retry failed payment with new payment method
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetryPaymentService implements RetryPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventBus eventBus;
    // private final InvoiceService invoiceService; // To get invoice details

    @Override
    @Transactional
    public PaymentResult retry(RetryPaymentCommand command) {
        log.info("Retrying payment for invoice: {}", command.invoiceId());

        // 1. Check for active payment
        InvoiceId invoiceId = new InvoiceId(command.invoiceId());
        List<Payment> existingPayments = paymentRepository.findAllByInvoiceId(invoiceId);

        if (existingPayments.size() > 10) {
            throw new TooManyPaymentAttemptsException(
                    "Invoice has exceeded maximum payment attempts. Please contact support.");
        }
        // 2. Get invoice details (amount, currency)
        // InvoiceDetails invoice =
        // invoiceService.getInvoiceDetails(command.invoiceId());
        Money amount = new Money(new BigDecimal(5), "USD");

        // 3. Create new payment (PENDING)
        Payment payment = Payment.createPendingPayment(invoiceId, amount);
        Payment savedPayment = paymentRepository.save(payment);

        log.info("New payment created for retry: {}", savedPayment.getId());

        // 4. Attempt authorization with new payment method
        try {
            String gatewayReferenceId = paymentGateway.authorize(
                    savedPayment.getId(),
                    command.customerId(),
                    command.paymentMethodId(), // New payment method
                    new BigDecimal(5),
                    "USD");

            savedPayment.authorize(gatewayReferenceId);
            paymentRepository.save(savedPayment);

            log.info("Payment retry successful: {}", savedPayment.getId());

        } catch (PaymentGatewayException e) {
            log.error("Payment retry failed: {} - Reason: {}",
                    savedPayment.getId(), e.getMessage());

            savedPayment.markAsFailed(e.getMessage());
            paymentRepository.save(savedPayment);
        }

        // 5. Publish events
        if (!savedPayment.getDomainEvents().isEmpty()) {
            eventBus.publish(savedPayment.getDomainEvents());
            savedPayment.clearDomainEvents();
        }

        return PaymentResult.fromDomain(savedPayment);
    }
}