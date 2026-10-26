package com.paymenthub.payment_service.infrastructure.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.PublishFailedException;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.UnroutableMessageException;

/**
 * Publishes with mandatory=true and blocks until the broker confirm arrives.
 * Returns normally only if the broker acked AND routed the message; nack,
 * return, timeout and connection errors all throw, so the caller (outbox
 * relay, retry/DLQ hand-off) never treats a lost message as sent.
 * Requires publisher-confirm-type=correlated and publisher-returns=true.
 */
@Component
public class ConfirmingPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long timeoutMs;

    public ConfirmingPublisher(RabbitTemplate rabbitTemplate,
            @Value("${messaging.publish-timeout-ms:10000}") long timeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutMs = timeoutMs;
        this.rabbitTemplate.setMandatory(true);
    }

    public void publish(String exchange, String routingKey, byte[] body, MessageProperties properties) {
        CorrelationData correlation = new CorrelationData(properties.getMessageId());
        try {
            rabbitTemplate.send(exchange, routingKey, new Message(body, properties), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(timeoutMs, TimeUnit.MILLISECONDS);
            // Spring sets the returned message before completing the future
            if (correlation.getReturned() != null) {
                throw new UnroutableMessageException(
                        "no queue bound for " + exchange + "/" + routingKey
                                + " (" + correlation.getReturned().getReplyText() + ")");
            }
            if (!confirm.ack()) {
                throw new PublishFailedException("broker nacked: " + confirm.reason(), null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishFailedException("interrupted waiting for confirm", e);
        } catch (TimeoutException e) {
            throw new PublishFailedException("no confirm within " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            throw new PublishFailedException("confirm failed", e.getCause());
        } catch (UnroutableMessageException | PublishFailedException e) {
            throw e;
        } catch (RuntimeException e) {
            // AmqpException etc. - connection refused, channel closed...
            throw new PublishFailedException(e.getMessage(), e);
        }
    }
}
