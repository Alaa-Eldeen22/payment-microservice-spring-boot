package com.paymenthub.payment_service.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymenthub.payment_service.application.dto.result.PaymentResult;
import com.paymenthub.payment_service.application.exception.PaymentNotFoundException;
import com.paymenthub.payment_service.application.port.in.command.RetryPaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentsByInvoiceUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.RetryPaymentUseCase;
import com.paymenthub.payment_service.infrastructure.adapter.in.response.PaymentResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GetPaymentsByInvoiceUseCase getPaymentsByInvoiceUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final RetryPaymentUseCase retryPaymentUseCase;

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(@PathVariable String invoiceId) {
        log.info("Fetching payments for invoice: {}", invoiceId);
        List<PaymentResponse> payments = getPaymentsByInvoiceUseCase.getPaymentsByInvoice(invoiceId)
                .stream()
                .map(PaymentResponse::fromResult)
                .toList();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String paymentId) {
        try {
            log.info("Fetching payment with ID: {}", paymentId);

            return ResponseEntity.ok(PaymentResponse.fromResult(getPaymentUseCase.getPaymentById(paymentId)));
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching payment with ID: {}", paymentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/retry")
    public ResponseEntity<PaymentResponse> retryPayment(
            @Valid @RequestBody Object request) {

        log.info("Manual payment retry for invoice: {}", "");

        RetryPaymentCommand command = new RetryPaymentCommand(
                "",
                "",
                "");

        PaymentResult result = retryPaymentUseCase.retry(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PaymentResponse.fromResult(result));
    }
}