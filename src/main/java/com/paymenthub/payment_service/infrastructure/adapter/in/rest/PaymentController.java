package com.paymenthub.payment_service.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymenthub.payment_service.application.port.in.command.CapturePaymentCommand;
import com.paymenthub.payment_service.application.port.in.command.VoidPaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CapturePaymentUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.GetPaymentsByInvoiceUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.VoidPaymentUseCase;
import com.paymenthub.payment_service.domain.exception.IllegalPaymentStateException;
import com.paymenthub.payment_service.domain.exception.PaymentNotFoundException;
import com.paymenthub.payment_service.infrastructure.adapter.in.response.PaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GetPaymentsByInvoiceUseCase getPaymentsByInvoiceUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CapturePaymentUseCase capturePaymentUseCase;
    private final VoidPaymentUseCase voidPaymentUseCase;

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

    @PatchMapping("capture/{paymentId}")
    public ResponseEntity<String> capturePayment(@PathVariable String paymentId) {
        try {
            log.info("Capturing payment with ID: {}", paymentId);

            capturePaymentUseCase.capture(new CapturePaymentCommand(paymentId));

            return ResponseEntity.accepted().body("Payment capture is being processed");

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error capturing payment with ID: {}", paymentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PatchMapping("void/{paymentId}")
    public ResponseEntity<String> voidPayment(
            @PathVariable String paymentId) {
        try {
            log.info("Voiding payment with ID: {}", paymentId);

            voidPaymentUseCase.voidPayment(new VoidPaymentCommand(paymentId));

            return ResponseEntity.accepted().body("Payment void is being processed");

        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalPaymentStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error voiding payment with ID: {}", paymentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}