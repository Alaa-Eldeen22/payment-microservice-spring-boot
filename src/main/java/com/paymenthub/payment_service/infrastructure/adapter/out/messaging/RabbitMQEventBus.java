package com.paymenthub.payment_service.infrastructure.adapter.out.messaging;

import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RabbitMQEventBus implements EventBus {

    private final RabbitTemplate rabbitTemplate;

    @Value("${INVOICE_EXCHANGE:invoice_events}")
    private String paymentEventsExchange;

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        paymentEventsExchange,
                        event.getEventType(),
                        event);
                log.info("Published event: {} with ID: {}",
                        event.getEventType(), event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getEventType(), e);
                throw e;
            }
        }
    }
}
