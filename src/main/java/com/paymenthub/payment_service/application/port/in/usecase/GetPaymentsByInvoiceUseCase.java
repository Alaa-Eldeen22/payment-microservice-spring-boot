package com.paymenthub.payment_service.application.port.in.usecase;

import com.paymenthub.payment_service.application.dto.response.PaymentResponse;
import java.util.List;

public interface GetPaymentsByInvoiceUseCase {
    List<PaymentResponse> execute(String invoiceId);
}