package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.response.PaymentResponse;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentsByInvoiceUseCase;
import java.util.List;

public class GetPaymentsByInvoiceService implements GetPaymentsByInvoiceUseCase {

    @Override
    public List<PaymentResponse> getPaymentsByInvoice(String invoiceId) {
        return null;
    }

}
