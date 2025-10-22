package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.RetryPaymentCommand;
import com.paymenthub.payment_service.application.dto.result.PaymentResult;

/**
 * Use case for manual payment retry
 */
public interface RetryPaymentUseCase {
    PaymentResult retry(RetryPaymentCommand command);
}