package com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Transactional outbox row, see MESSAGING-CONTRACT.md §7. */
@Entity
@Table(name = "outbox_messages", indexes = @Index(name = "outbox_messages_pending_idx", columnList = "next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxMessageEntity {

    /** = envelope.messageId */
    @Id
    private UUID id;

    @Column(nullable = false)
    private String exchange;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    /** serialized envelope, published byte-for-byte */
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
}
