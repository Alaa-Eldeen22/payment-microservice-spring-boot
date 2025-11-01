package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.VoidPaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.VoidPaymentUseCase;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.exception.IllegalPaymentStateException;
import com.paymenthub.payment_service.domain.exception.PaymentNotFoundException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoidPaymentService implements VoidPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventBus eventBus;

    @Override
    @Transactional
    public void voidPayment(VoidPaymentCommand command) {
        log.info("Voiding payment: {}", command.paymentId());

        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        try {
            paymentGateway.voidAuthorization(payment.getPaymentGatewayReferenceId());

            payment.voidAuthorization();

            paymentRepository.save(payment);

            if (!payment.getDomainEvents().isEmpty()) {
                eventBus.publish(payment.getDomainEvents());
                payment.clearDomainEvents();
            }

            log.info("Successfully voided payment: {}", payment.getId());

        } catch (PaymentGatewayException e) {
            log.error("Payment gateway void failed - Error: {}, Code: {}, Gateway Message: {}",
                    e.getMessage(), e.getErrorCode(), e.getGatewayMessage());
            throw e;

        } catch (IllegalPaymentStateException e) {
            log.error("Invalid payment state for void: {}", payment.getId());
            throw e;
        }
    }
}