package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.request.ProcessInvoiceRequest;
import com.paymenthub.payment_service.application.dto.response.PaymentResponse;

public interface ProcessInvoiceCreatedUseCase {
    PaymentResponse execute(ProcessInvoiceRequest request);
}