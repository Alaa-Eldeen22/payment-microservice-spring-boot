package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.paymenthub.payment_service.infrastructure.messaging.MessageEnvelope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcessedMessageStore {

    private final JdbcTemplate jdbc;

    /**
     * Records the message as processed by {@code consumer} in the caller's
     * transaction. Returns false if it was already recorded (duplicate).
     * Concurrent duplicates block on the PK until the first tx finishes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryRecord(String consumer, MessageEnvelope envelope) {
        int inserted = jdbc.update("""
                INSERT INTO processed_messages (consumer, idempotency_key, message_id, event_type, processed_at)
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (consumer, idempotency_key) DO NOTHING
                """,
                consumer, envelope.idempotencyKey(), UUID.fromString(envelope.messageId()), envelope.eventType());
        return inserted > 0;
    }

    public int deleteProcessedBefore(Instant before) {
        return jdbc.update("DELETE FROM processed_messages WHERE processed_at < ?",
                java.sql.Timestamp.from(before));
    }
}
