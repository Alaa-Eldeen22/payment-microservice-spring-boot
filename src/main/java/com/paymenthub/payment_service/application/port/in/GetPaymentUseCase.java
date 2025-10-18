package com.paymenthub.payment_service.application.port.in;

import com.paymenthub.payment_service.application.dto.response.PaymentResponse;

public interface GetPaymentUseCase {
    PaymentResponse execute(String paymentId);
}