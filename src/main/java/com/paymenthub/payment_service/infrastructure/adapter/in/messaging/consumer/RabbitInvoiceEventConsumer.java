package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.CreatePaymentUseCase;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceCreatedEvent;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceRetryEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitInvoiceEventConsumer {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final CreateAndAuthorizePaymentUseCase createAndAuthorizePaymentUseCase;

    /**
     * Listens to invoice.created events from Invoice Service
     * Automatically creates a payment when an invoice is created
     */
    @RabbitListener(queues = "invoice_events")
    public void handleInvoiceCreated(InvoiceCreatedEvent event) {
        log.info("Received InvoiceCreatedEvent: invoiceId={}, amount={} {}",
                event.invoiceId(), event.amount(), event.currency());

        try {
            CreateAndAuthorizePaymentCommand command = new CreateAndAuthorizePaymentCommand(
                    event.invoiceId(),
                    event.customerId(),
                    event.amount(),
                    event.currency(),
                    "event.paymentMethodId()");

            createAndAuthorizePaymentUseCase.execute(command);
            log.info("Successfully created payment for invoice: {}", event.invoiceId());

        } catch (DuplicatePaymentException e) {
            log.warn("Payment already exists for invoice: {} - skipping", event.invoiceId());

        } catch (IllegalArgumentException e) {
            log.error("Invalid data in InvoiceCreatedEvent: {}", event.invoiceId(), e);
            throw e;

        } catch (Exception e) {
            log.error("Failed to process InvoiceCreatedEvent: {}", event.invoiceId(), e);
            throw e;
        }
    }

    /**
     * Listens to invoice.retry events from Invoice Service
     * Creates a new payment attempt when customer retries a failed invoice
     */
    @RabbitListener(queues = "invoice_events", containerFactory = "headers['amqp_receivedRoutingKey'] == 'invoice.retry'")
    public void handleInvoiceRetry(InvoiceRetryEvent event) {
        // log.info("Received InvoiceRetryEvent: invoiceId={}, previousPaymentId={}, amount={} {}", 
        //         event.invoiceId(), event.previousPaymentId(), event.amount(), event.currency());

        try {
            CreateAndAuthorizePaymentCommand command = new CreateAndAuthorizePaymentCommand(
                    event.invoiceId(),
                    event.customerId(),
                    event.amount(),
                    event.currency(),
                    "event.paymentMethodId()");

            createAndAuthorizePaymentUseCase.execute(command);
            log.info("Successfully created retry payment for invoice: {}", event.invoiceId());

        } catch (DuplicatePaymentException e) {
            log.warn("Active payment already exists for invoice: {} - skipping retry", event.invoiceId());
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Invalid data in InvoiceRetryEvent: {}", event.invoiceId(), e);
            throw e;

        } catch (Exception e) {
            log.error("Failed to process InvoiceRetryEvent: {}", event.invoiceId(), e);
            throw e;
        }
    }
}
