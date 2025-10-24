package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import com.paymenthub.payment_service.application.port.in.command.CreateAndAuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceCreatedEvent;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.event.InvoiceRetriedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitInvoiceEventConsumer {

    private static final String INVOICE_CREATED = "invoice.created";
    private static final String INVOICE_RETRIED = "invoice.retried";

    private final CreateAndAuthorizePaymentUseCase createAndAuthorizePaymentUseCase;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "invoice_events")
    public void handleMessage(Message message, @Header("amqp_receivedRoutingKey") String routingKey) {
        try {
            switch (routingKey) {
                case INVOICE_CREATED -> handleInvoiceCreated(message);
                case INVOICE_RETRIED -> handleInvoiceRetried(message);
                default -> log.warn("Received unknown routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to handle message for routing key: {}", routingKey, e);
            throw new RuntimeException("Message handling failed for routing key: " + routingKey, e);
        }
    }

    private void handleInvoiceCreated(Message message) throws Exception {
        InvoiceCreatedEvent event = objectMapper.readValue(message.getBody(), InvoiceCreatedEvent.class);
        log.info("Received InvoiceCreatedEvent: invoiceId={}, amount={} {}, paymentMethodId={}",
                event.invoiceId(), event.amount(), event.currency(), event.paymentMethodId());

        processCreateAndAuthorize(
                event.invoiceId(),
                event.customerId(),
                event.amount(),
                event.currency(),
                event.paymentMethodId(),
                INVOICE_CREATED);
    }

    private void handleInvoiceRetried(Message message) throws Exception {
        InvoiceRetriedEvent event = objectMapper.readValue(message.getBody(), InvoiceRetriedEvent.class);
        log.info("Received InvoiceRetriedEvent: invoiceId={}, amount={} {}, paymentMethodId={}",
                event.invoiceId(), event.amount(), event.currency(), event.paymentMethodId());

        processCreateAndAuthorize(
                event.invoiceId(),
                event.customerId(),
                event.amount(),
                event.currency(),
                event.paymentMethodId(),
                INVOICE_RETRIED);
    }

    private void processCreateAndAuthorize(
            String invoiceId,
            String customerId,
            BigDecimal amount,
            String currency,
            String paymentMethodId,
            String eventType) {
        try {
            CreateAndAuthorizePaymentCommand command = new CreateAndAuthorizePaymentCommand(
                    invoiceId,
                    customerId,
                    amount,
                    currency,
                    paymentMethodId);

            createAndAuthorizePaymentUseCase.createAndAuthorize(command);
            log.info("Successfully processed {} payment for invoice: {}", eventType, invoiceId);

        } catch (DuplicatePaymentException e) {
            log.warn("Duplicate payment detected for invoice: {} - skipping processing", invoiceId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid data in {} event for invoice: {}", eventType, invoiceId, e);
        } catch (PaymentGatewayException e) {
            log.error("Payment gateway error while processing {} event for invoice: {}", eventType, invoiceId, e);

        } catch (Exception e) {
            log.error("Unexpected failure while processing {} event for invoice: {}", eventType, invoiceId, e);
            throw new RuntimeException("Failed to process " + eventType + " event for invoice: " + invoiceId, e);
        }
    }
}
