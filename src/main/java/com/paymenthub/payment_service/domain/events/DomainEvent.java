package com.paymenthub.payment_service.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    public String getEventType() {
        EventType annotation = this.getClass().getAnnotation(EventType.class);
        if (annotation != null) {
            return annotation.value();
        }

        return this.getClass().getSimpleName();
    }
}