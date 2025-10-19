package com.paymenthub.payment_service.application.port.out;

import java.util.List;

import com.paymenthub.payment_service.domain.events.DomainEvent;

/**
 * Port for publishing domain events to message broker
 */

public interface EventBus {
    void publish(List<DomainEvent> events);
}