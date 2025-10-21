package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreatePaymentUseCase;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitInvoiceEventConsumer {

    private final CreatePaymentUseCase createPaymentUseCase;

    /**
     * Listens to invoice.created events from Invoice Service
     * Automatically creates a payment when an invoice is created
     */
    @RabbitListener(queues = "invoice_events")
    public void handleInvoiceCreated(InvoiceCreatedEvent event) {
        log.info("Received InvoiceCreatedEvent: invoiceId={}, amount={} {}",
                event.invoiceId(), event.amount(), event.currency());

        try {
            CreatePaymentCommand command = new CreatePaymentCommand(
                    event.invoiceId(),
                    event.amount(),
                    event.currency());

            createPaymentUseCase.createPayment(command);

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
}
