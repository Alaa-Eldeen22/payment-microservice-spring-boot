package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.INVOICE_EVENTS_QUEUE;
import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.MAX_RETRIES;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.application.port.in.usecase.CreateAndAuthorizePaymentUseCase;
import com.paymenthub.payment_service.domain.exception.DuplicatePaymentException;
import com.paymenthub.payment_service.domain.exception.TooManyPaymentAttemptsException;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer.MessageProcessor.Outcome;
import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox.ProcessedMessageStore;
import com.paymenthub.payment_service.infrastructure.messaging.ConfirmingPublisher;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingLog;
import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;

/**
 * Manual-ack consumer of payment-service.invoice-events (contract §3.3).
 * Acks only after the processor reports the message handled or safely
 * handed to retry/DLQ; otherwise nack+requeue.
 */
@Slf4j
@Component
public class InvoiceEventsListener {

    private final MessageProcessor processor;

    public InvoiceEventsListener(CreateAndAuthorizePaymentUseCase useCase, TransactionTemplate tx,
            ProcessedMessageStore dedup, ConfirmingPublisher publisher, ObjectMapper objectMapper) {
        this.processor = new MessageProcessor(
                new MessageProcessor.Config(
                        INVOICE_EVENTS_QUEUE,
                        InvoiceEventHandlers.create(useCase),
                        MAX_RETRIES,
                        // retrying can't fix these
                        e -> e instanceof TooManyPaymentAttemptsException || e instanceof IllegalArgumentException,
                        // an active payment already exists for the invoice: nothing to do
                        e -> e instanceof DuplicatePaymentException),
                tx, dedup, publisher, objectMapper);
    }

    @RabbitListener(queues = INVOICE_EVENTS_QUEUE, ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        Outcome outcome;
        try {
            outcome = processor.process(message);
        } catch (RuntimeException e) {
            // processor isn't supposed to throw; be defensive anyway
            log.error("processor threw, requeueing: {}", MessagingLog.describe(e));
            outcome = Outcome.REQUEUE;
        }
        if (outcome == Outcome.ACK) {
            channel.basicAck(tag, false);
        } else {
            channel.basicNack(tag, false, true);
        }
    }
}
