package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.consumer;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;

/** Handles one eventType on a consumer queue. */
public interface EventHandler {

    /** payload schemaVersions this handler understands */
    Set<Integer> versions();

    /** throw MalformedMessageException if the payload doesn't match the contract */
    void validate(JsonNode payload, int version);

    /** runs inside the dedup transaction */
    void handle(MessageEnvelope envelope);
}
