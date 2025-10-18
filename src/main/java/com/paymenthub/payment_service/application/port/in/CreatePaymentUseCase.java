package com.paymenthub.payment_service.application.port.in;

import com.paymenthub.payment_service.application.dto.request.CreatePaymentRequest;
import com.paymenthub.payment_service.application.dto.response.PaymentResponse;

public interface CreatePaymentUseCase {
    PaymentResponse execute(CreatePaymentRequest request);
}