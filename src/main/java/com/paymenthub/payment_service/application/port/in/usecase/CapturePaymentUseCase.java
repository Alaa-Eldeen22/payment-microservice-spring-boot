package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.CapturePaymentCommand;

public interface CapturePaymentUseCase {
    void capture(CapturePaymentCommand request);
}