package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.request.CapturePaymentRequest;
import com.paymenthub.payment_service.application.dto.response.CaptureResponse;

public interface CapturePaymentUseCase {
    CaptureResponse execute(CapturePaymentRequest request);
}