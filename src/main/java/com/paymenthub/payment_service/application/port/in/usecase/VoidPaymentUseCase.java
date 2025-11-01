package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.VoidPaymentCommand;

public interface VoidPaymentUseCase {
    void voidPayment(VoidPaymentCommand command);
}