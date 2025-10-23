package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.command.CreatePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.application.port.in.usecase.CreatePaymentUseCase;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitInvoiceEventConsumer {

    private final CreateAndAuthorizePaymentUseCase createAndAuthorizePaymentUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "invoice_events")
    public void handleMessage(Message message, @Header("amqp_receivedRoutingKey") String routingKey) {

        try {
            switch (routingKey) {
                case "invoice.created":
                    log.info("creating handler");
                    InvoiceCreatedEvent event = objectMapper.readValue(
                            message.getBody(),
                            InvoiceCreatedEvent.class);
                    handleInvoiceCreatedEvent(event);
                    break;

                case "invoice.retry":
                    log.info("retry handler");
                    // Handle retry logic
                    break;

                default:
                    log.warn("Unknown routing key: {}", routingKey);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to deserialize message for routing key: {}", routingKey, e);
            throw new RuntimeException("Message deserialization failed", e);
        }
    }

    public void handleInvoiceCreatedEvent(InvoiceCreatedEvent event) {
        log.info("Received InvoiceCreatedEvent: invoiceId={}, amount={} {}",
                event.invoiceId(), event.amount(), event.currency());
        try {
            CreateAndAuthorizePaymentCommand command = new CreateAndAuthorizePaymentCommand(
                    event.invoiceId(),
                    event.customerId(),
                    event.amount(),
                    event.currency(),
                    event.paymentMethodId()

            );

            // CreatePaymentCommand command = new CreatePaymentCommand(
            // event.invoiceId(),
            // event.amount(),
            // event.currency());

            // createPaymentUseCase.createPayment(command);

            createAndAuthorizePaymentUseCase.createAndAuthorize(command);
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