package com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, UUID> {

    /** Locks a batch of due rows; other relay instances skip them. */
    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE published_at IS NULL AND next_attempt_at <= now()
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessageEntity> lockPendingBatch(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM OutboxMessageEntity o WHERE o.publishedAt IS NOT NULL AND o.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
