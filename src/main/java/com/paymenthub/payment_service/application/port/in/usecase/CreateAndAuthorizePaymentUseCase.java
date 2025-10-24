package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.dto.result.PaymentResult;

/**
 * Use case for automatic payment flow
 * Creates payment and immediately attempts authorization
 */
public interface CreateAndAuthorizePaymentUseCase {
    PaymentResult createAndAuthorize(CreatePaymentCommand command);
}