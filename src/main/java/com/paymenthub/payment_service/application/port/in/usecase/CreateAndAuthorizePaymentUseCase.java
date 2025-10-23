package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.dto.result.PaymentResult;

/**
 * Use case for automatic payment flow
 * Creates payment and immediately attempts authorization
 */
public interface CreateAndAuthorizePaymentUseCase {
    PaymentResult createAndAuthorize(CreateAndAuthorizePaymentCommand command);
}