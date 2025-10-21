package com.paymenthub.payment_service.application.service;

import org.springframework.stereotype.Service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentUseCase;
import com.paymenthub.payment_service.application.exception.PaymentNotFoundException;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResult getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResult::fromDomain)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
