package com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Consumer dedup record, see MESSAGING-CONTRACT.md §6. Mapped only so the
 * table gets created; rows are written with a native ON CONFLICT insert.
 */
@Entity
@Table(name = "processed_messages", indexes = @Index(name = "processed_messages_processed_at_idx", columnList = "processed_at"))
@IdClass(ProcessedMessageEntity.Key.class)
@Getter
@NoArgsConstructor
public class ProcessedMessageEntity {

    @Id
    @Column(length = 100)
    private String consumer;

    @Id
    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String consumer;
        private String idempotencyKey;
    }
}
