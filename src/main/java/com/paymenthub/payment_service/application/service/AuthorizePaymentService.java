package com.paymenthub.payment_service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.exception.PaymentNotFoundException;
import com.paymenthub.payment_service.application.port.in.command.AuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.AuthorizePaymentUseCase;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorizePaymentService implements AuthorizePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final EventBus eventBus;

    @Override
    @Transactional
    public PaymentResult authorize(AuthorizePaymentCommand command) {
        log.info("Authorizing payment: {} with intent: {}",
                command.paymentId(), command.paymentGatewayReferenceId());

        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + command.paymentId()));

        payment.authorize(command.paymentGatewayReferenceId());

        Payment savedPayment = paymentRepository.save(payment);

        if (!savedPayment.getDomainEvents().isEmpty()) {
            eventBus.publish(savedPayment.getDomainEvents());
            savedPayment.clearDomainEvents();
        }

        log.info("Payment authorized successfully: {} expires at: {}",
                savedPayment.getId(), savedPayment.getExpiresAt());

        return PaymentResult.fromDomain(savedPayment);
    }
}