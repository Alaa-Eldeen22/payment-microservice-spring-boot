# Messaging runbook — payment-service

This page is for operators. The design is in [MESSAGING-CONTRACT.md](https://github.com/Alaa-Eldeen22/invoice-microservice-nestjs/blob/main/docs/MESSAGING-CONTRACT.md) (locally `../../MESSAGING-CONTRACT.md`); this page covers what to look at and what to do. The cross-service verification run is documented in the invoice repo, in `docs/messaging-verification.md`.

## What this service owns

| Object | Purpose |
|---|---|
| exchange `payment.events` (topic) | where this service publishes `payment.authorized/failed/captured/voided` |
| queue `payment-service.invoice-events` (quorum) | `invoice.created` and `invoice.retried` from the invoice service |
| `payment-service.invoice-events.retry.{1,2,3}` | backoff holding queues (5 s / 30 s / 120 s TTL); they hold messages briefly and should drain by themselves |
| `payment-service.invoice-events.dlq` (quorum) | dead letters. **Anything here needs a human.** |
| table `outbox_messages` | events waiting to be published (`published_at IS NULL`) |
| table `processed_messages` | dedup records (30-day retention) |

## What to watch

| Signal | Healthy | Investigate when |
|---|---|---|
| DLQ depth | 0 | > 0, always |
| Main queue ready count | near 0 | growing: the consumer is down or slow (the gateway call is inside the handler) |
| Main queue unacked | ≤ prefetch (10) | stuck at prefetch: gateway or DB hanging |
| Retry queues | short bursts | constantly non-empty: a dependency is flapping |
| Outbox backlog | 0 or a few rows < 2 s old | rows older than a minute |

```sql
SELECT event_type, count(*), min(created_at) AS oldest, max(attempts) AS max_attempts,
       left(max(last_error), 120) AS sample_error
FROM outbox_messages WHERE published_at IS NULL GROUP BY event_type;
```

## Following one message

Every messaging step logs `event key=value…` with `messageId`, `correlationId`, `eventType`, `idempotencyKey`, `queue` and `retryCount`. Every log line also carries `cid=<correlationId> mid=<messageId>` from the MDC, including business log lines written while a message is being handled.

For JSON logs, set `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`. That's Spring Boot structured logging, and it includes the MDC fields.

```bash
grep 'correlationId=<id>\|cid=<id>' payment.log invoice.log
```

REST calls accept and return `X-Correlation-Id`.

## Failure modes as they look in the logs

| Log event | Meaning | Action |
|---|---|---|
| `outbox.enqueued` | Event stored with the business change | none |
| `rmq.publish.ok` | Broker confirmed and routed it | none |
| `rmq.publish.failed … PublishFailedException: …ConnectException` / `no confirm within` | Broker unreachable or slow. The row stays pending and is retried with backoff (2 s … 60 s). | Check the broker. Nothing is lost. |
| `rmq.publish.returned … UnroutableMessageException` | No queue bound for the routing key. The row stays pending. | The invoice service isn't deployed or its queue/binding is gone. Start it, and the backlog drains. |
| `rmq.consume.ok` | Processed, committed, acked. With `noop=DuplicatePaymentException…`, an active payment already existed, so nothing was done (by design). | none |
| `rmq.consume.duplicate` | Same `idempotencyKey` was already processed. | none |
| `rmq.consume.retry_scheduled retryCount=n` | Handler failed (DB, gateway timeout…). A copy waits in `.retry.n`. | Look at `error=` |
| `rmq.consume.dead_lettered reason=…` | Gave up. The message is in the DLQ. | See below. |
| `rmq.consume.requeued` | Couldn't hand the message to retry or DLQ. Nacked back to the main queue. | Broker trouble. After 10 deliveries the broker DLQs it. |

A gateway **decline** is not a failure. It commits a FAILED payment, publishes `payment.failed`, and acks.

`x-dlq-reason` values:

| Reason | Typical cause | Replay? |
|---|---|---|
| `max_retries_exceeded` | DB or gateway down for minutes | Yes, after the dependency recovers |
| `non_retryable_error` | `TooManyPaymentAttemptsException` (10 attempts per invoice), or `IllegalArgumentException` (for example an invalid currency code) | Usually no. It's a business or data problem. |
| `malformed_message` | Body or payload violates the contract | No. Fix the producer, then discard. |
| `unsupported_schema_version` / `unknown_event_type` | Producer ahead of this consumer | After deploying support |
| *(none)*, but `x-first-death-reason=delivery_limit` | The consumer died 10 times on this message | Investigate the crash first |

## Inspecting, replaying, discarding

Use the script that lives with the contract in the invoice repo, `scripts/rmq-dlq.py` (Python stdlib only, talks to the management API):

```bash
export RABBITMQ_MGMT_URL=http://rabbit:15672 RABBITMQ_MGMT_USER=… RABBITMQ_MGMT_PASSWORD=…
rmq-dlq.py list    payment-service.invoice-events
rmq-dlq.py replay  payment-service.invoice-events --message-id <id> --dry-run
rmq-dlq.py replay  payment-service.invoice-events --message-id <id>     # or --all
rmq-dlq.py discard payment-service.invoice-events --message-id <id>
```

- Every message is written to `./dlq-backup/` before it leaves the DLQ.
- Replays go to the default exchange with routing key `payment-service.invoice-events` (this consumer only). The retry budget is reset; body and ids are unchanged.
- Replaying something that was actually processed logs `rmq.consume.duplicate`. For `invoice.created`, the "an active payment already exists" check also guards it.
- **Fix the cause before replaying.** Never replay by publishing to the `invoice.events` topic exchange.
- In the management UI, *Get messages* with **Nack message requeue true** only.

## Other procedures

- **Changing a retry TTL or queue argument:** stop consumers, delete the empty queue, restart. The app re-declares it (`MessagingTopologyConfig`). Update the contract first.
- **Retention:** the hourly `MessagingHousekeeping` job deletes `processed_messages` older than 30 days and published outbox rows older than 7 days.
- **Real gateway integration:** pass a gateway idempotency key derived from the envelope `idempotencyKey`. Otherwise a crash between the gateway call and the commit authorizes twice on redelivery (contract §6).
- **Legacy cleanup** (after the cutover, contract §8.1): delete the old `invoice_events` exchange and queue and the `payment_events` queue once they're empty.
