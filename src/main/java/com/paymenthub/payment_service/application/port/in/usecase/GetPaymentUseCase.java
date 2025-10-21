package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;

public interface GetPaymentUseCase {
    PaymentResult getPaymentById(String paymentId);
}