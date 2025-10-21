package com.paymenthub.payment_service.application.port.in.usecase;

import java.util.List;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;

public interface GetPaymentsByInvoiceUseCase {
    List<PaymentResult> getPaymentsByInvoice(String invoiceId);
}