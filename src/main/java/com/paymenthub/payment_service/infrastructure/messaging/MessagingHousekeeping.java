package com.paymenthub.payment_service.infrastructure.messaging;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.paymenthub.payment_service.infrastructure.adapter.in.messaging.inbox.ProcessedMessageStore;
import com.paymenthub.payment_service.infrastructure.adapter.out.messaging.outbox.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Retention from the contract: dedup rows 30 days (§6), published outbox
 * rows 7 days (§7). Unpublished outbox rows are never deleted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingHousekeeping {

    private final ProcessedMessageStore processedMessages;
    private final OutboxMessageRepository outbox;

    @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void purge() {
        Instant now = Instant.now();
        int dedup = processedMessages.deleteProcessedBefore(now.minus(Duration.ofDays(30)));
        int sent = outbox.deletePublishedBefore(now.minus(Duration.ofDays(7)));
        log.info("purged processed_messages={} outbox_messages={}", dedup, sent);
    }
}
