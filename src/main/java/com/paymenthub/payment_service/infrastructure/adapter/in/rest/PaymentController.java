package com.paymenthub.payment_service.infrastructure.adapter.in.rest;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentsByInvoiceUseCase;
import com.paymenthub.payment_service.infrastructure.adapter.in.response.PaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GetPaymentsByInvoiceUseCase getPaymentsByInvoiceUseCase;

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(@PathVariable String invoiceId) {
        log.info("Fetching payments for invoice: {}", invoiceId);
        List<PaymentResponse> payments = getPaymentsByInvoiceUseCase.getPaymentsByInvoice(invoiceId)
                .stream()
                .map(PaymentResponse::fromResult)
                .toList();
        return ResponseEntity.ok(payments);
    }
}