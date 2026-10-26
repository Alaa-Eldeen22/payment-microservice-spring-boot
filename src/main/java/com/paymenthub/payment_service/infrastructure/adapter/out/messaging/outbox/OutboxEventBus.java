package com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingLog.fields;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymenthub.payment_service.application.port.out.EventBus;
import com.paymenthub.payment_service.domain.events.DomainEvent;
import com.paymenthub.payment_service.infrastructure.messaging.MessageContext;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EventBus adapter that never talks to the broker: it writes contract
 * envelopes to the outbox in the caller's transaction (MANDATORY - there is
 * no way to "publish" outside a business transaction). OutboxRelay sends them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventBus implements EventBus {

    private final OutboxMessageRepository repository;
    private final PaymentEventMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(List<DomainEvent> events) {
        MessageContext.Data ctx = MessageContext.current()
                .orElseGet(() -> new MessageContext.Data(UUID.randomUUID().toString(), null));

        for (DomainEvent event : events) {
            MessageEnvelope envelope = mapper.toEnvelope(event, ctx).orElse(null);
            if (envelope == null) {
                log.debug("not in contract, skipping {}", event.getEventType());
                continue;
            }
            Instant now = Instant.now();
            OutboxMessageEntity row = new OutboxMessageEntity();
            row.setId(UUID.fromString(envelope.messageId()));
            row.setExchange(MessagingConstants.PAYMENT_EVENTS_EXCHANGE);
            row.setRoutingKey(envelope.eventType());
            row.setEventType(envelope.eventType());
            row.setCorrelationId(envelope.correlationId());
            row.setBody(envelope.toJson(objectMapper));
            row.setCreatedAt(now);
            row.setNextAttemptAt(now);
            repository.save(row);

            MessagingLog.log(log, Level.INFO, "outbox.enqueued", fields(
                    "messageId", envelope.messageId(),
                    "correlationId", envelope.correlationId(),
                    "eventType", envelope.eventType()));
        }
    }
}
