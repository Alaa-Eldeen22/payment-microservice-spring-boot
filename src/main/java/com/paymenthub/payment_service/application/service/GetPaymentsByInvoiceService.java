package com.paymenthub.payment_service.application.service;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentsByInvoiceUseCase;
import com.paymenthub.payment_service.domain.repository.PaymentRepository;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPaymentsByInvoiceService implements GetPaymentsByInvoiceUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public List<PaymentResult> getPaymentsByInvoice(String invoiceId) {

        return paymentRepository.findAllByInvoiceId(new InvoiceId(invoiceId)).stream().map(PaymentResult::fromDomain)
                .toList();
    }

}
