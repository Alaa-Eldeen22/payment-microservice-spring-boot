package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.CapturePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CapturePaymentUseCase;
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
public class CapturePaymentService implements CapturePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventBus eventBus;

    @Override
    @Transactional
    public void capture(CapturePaymentCommand request) {
        log.info("Capturing payment: {}", request.paymentId());

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(request.paymentId()));

        try {
            
            paymentGateway.capture(payment.getPaymentGatewayReferenceId());

            payment.capture();

            paymentRepository.save(payment);

            if (!payment.getDomainEvents().isEmpty()) {
                eventBus.publish(payment.getDomainEvents());
                payment.clearDomainEvents();
            }

            log.info("Successfully captured payment: {}", payment.getId());

        } catch (PaymentGatewayException e) {
            log.error("Payment gateway capture failed - Error: {}, Code: {}, Gateway Message: {}",
                    e.getMessage(), e.getErrorCode(), e.getGatewayMessage());
            throw e;

        } catch (IllegalPaymentStateException e) {
            log.error("Invalid payment state for capture: {}", payment.getId());
            throw e;
        }
    }
}
