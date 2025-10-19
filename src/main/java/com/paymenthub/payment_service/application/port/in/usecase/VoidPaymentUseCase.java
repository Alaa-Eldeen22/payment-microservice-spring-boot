package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.request.VoidPaymentRequest;
import com.paymenthub.payment_service.application.dto.response.VoidResponse;

public interface VoidPaymentUseCase {
    VoidResponse execute(VoidPaymentRequest request);
}