package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.port.in.command.AuthorizePaymentCommand;

public interface AuthorizePaymentUseCase {
    PaymentResult authorize(AuthorizePaymentCommand command);
}