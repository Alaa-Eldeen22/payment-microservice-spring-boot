package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizePaymentService {

    private final PaymentRepository paymentRepository;
    private final EventBus eventBus;
    private final PaymentGateway paymentGateway;

    @Transactional
    protected Payment authorize(Payment payment) {
        log.info("Starting authorization for payment ID: {}", payment.getId());

        try {
            String gatewayReferenceId = authorizeWithGateway(payment);

            payment.authorize(gatewayReferenceId);
            Payment savedPayment = paymentRepository.save(payment);

            publishDomainEvents(savedPayment);

            log.info("Payment ID: {} authorized successfully with gateway reference: {}",
                    savedPayment.getId(), gatewayReferenceId);

            return savedPayment;

        } catch (PaymentGatewayException e) {
            handleAuthorizationFailure(payment, e);
            // throw e;
        }
        return payment;
    }

    private String authorizeWithGateway(Payment payment) {
        return paymentGateway.authorize(
                payment.getId(),
                "", // TODO: Pass customer Id when added
                payment.getPaymentMethodId().getValue(),
                payment.getRequestedAmount().getAmount(),
                payment.getRequestedAmount().getCurrency().toString());
    }

    private void handleAuthorizationFailure(Payment payment, PaymentGatewayException exception) {
        log.error("Authorization failed for payment ID: {} - Reason: {}",
                payment.getId(), exception.getMessage());

        payment.markAsFailed(exception.getMessage());
        paymentRepository.save(payment);

        // publishDomainEvents(payment);
    }

    private void publishDomainEvents(Payment payment) {
        if (!payment.getDomainEvents().isEmpty()) {
            eventBus.publish(payment.getDomainEvents());
            payment.clearDomainEvents();
        }
    }
}
