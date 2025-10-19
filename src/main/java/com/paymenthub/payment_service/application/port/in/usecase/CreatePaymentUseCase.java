package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.result.PaymentResult;

public interface CreatePaymentUseCase {
    /**
     * Creates a new payment in PENDING status
     * 
     * @param command the payment creation command
     * @return result containing created payment details
     * @throws InvoiceNotFoundException   if invoice doesn't exist
     * @throws InvoiceNotPayableException if invoice cannot accept payment
     * @throws IllegalArgumentException   if command data is invalid (thrown by
     *                                    Value Objects)
     */
    PaymentResult createPayment(CreatePaymentCommand command);
}
