> **Historical.** This describes messaging *before* the `feature/rmq-resilience` work. For the current design see the [messaging contract](../../MESSAGING-CONTRACT.md), the README, and [messaging-runbook.md](messaging-runbook.md).

# Messaging notes — payment-microservice-spring-boot

State of the service **before** the `feature/rmq-resilience` work. These are investigation notes. The target design is in [`../../MESSAGING-CONTRACT.md`](../../MESSAGING-CONTRACT.md).

## Architecture

- Spring Boot 3.5.3, Java 17 target, Maven (wrapper checked in; `mvnw` is committed without the executable bit).
- Hexagonal layout:
  - `domain/`: `Payment` aggregate, value objects, domain events with an `@EventType` annotation.
  - `application/`: use cases, services, and the ports `EventBus` and `PaymentGateway`.
  - `infrastructure/adapter/{in,out}`: REST, RabbitMQ consumer and producer, JPA, and a dummy gateway.
- Persistence: PostgreSQL with Spring Data JPA, `ddl-auto=update`, no migrations. There is one table, `payments`.
- Messaging: `spring-boot-starter-amqp` (Spring AMQP / `RabbitTemplate` / `@RabbitListener`).
- Gateway: `DummyPaymentGateway` (`@Profile("dev")`). A payment method id of `fail` simulates a decline.

## RabbitMQ usage

### Topology (`infrastructure/config/RabbitMQConfig.java`)

The payment service declares **everything**, including the queue the invoice service consumes:

| Object | Name | Notes |
|---|---|---|
| Topic exchange | `invoice_events` (`INVOICE_EXCHANGE`) | durable. **Both** services publish here. |
| Queue | `invoice_events` (`INVOICE_QUEUE`) | durable, classic, no args. Has the same name as the exchange. |
| Queue | `payment_events` (`PAYMENT_QUEUE`) | durable, classic, no args. Consumed by the **invoice** service. |
| Bindings | `invoice_events` ← `invoice.created`, `invoice.retried`* | |
| Bindings | `payment_events` ← `payment.authorized/failed/captured/voided` | |

\* The default is `invoice.retried`, but `.env.example` sets `INVOICE_RETRIED_ROUTING_KEY=invoice.retry` (see Observations).

A `Jackson2JsonMessageConverter` bean is registered, so Boot wires it into both the `RabbitTemplate` and the listener factory. It uses its **own** `ObjectMapper` (`JacksonUtils.enhancedObjectMapper()`), so `spring.jackson.serialization.write-dates-as-timestamps=false` does **not** apply. `LocalDateTime` values go out as arrays.

### Publishing (`infrastructure/adapter/out/messaging/RabbitMQEventBus.java`)

- `rabbitTemplate.convertAndSend("invoice_events", event.getEventType(), event)`.
- The routing key comes from `@EventType`: `payment.authorized`, `payment.failed`, `payment.captured`, `payment.voided`.
- Body: Jackson dump of the domain event, for example `{eventId, occurredOn:[y,M,d,h,m,s,n], paymentId, invoiceId, eventType, reason?, capturedAt?}`, plus a `__TypeId__` header containing the Java class name.
- **No publisher confirms** (`publisher-confirm-type` is not set) and **no `mandatory`/returns**. `convertAndSend` only throws on channel or connection errors. A message the broker drops or can't route is invisible.

### Consuming (`infrastructure/adapter/in/messaging/consumer/RabbitInvoiceEventConsumer.java`)

- `@RabbitListener(queues = "invoice_events")`, queue name **hard-coded** (the `invoice.queue` property is ignored).
- Dispatch is by the `amqp_receivedRoutingKey` header:
  - `invoice.created` → `CreateAndAuthorizePaymentUseCase`
  - `invoice.retried` → the same use case
- The body is parsed with the Boot `ObjectMapper` into `InvoiceCreatedEvent`/`InvoiceRetriedEvent` records. `eventId` and `createdAt` are never sent by Node, so they are always `null`.
- **Ack mode: `AUTO`** (`spring.rabbitmq.listener.simple.acknowledge-mode=auto`). The container acks when the listener returns and rejects when it throws. `default-requeue-rejected` is the default `true`.
- Error handling:
  - `DuplicatePaymentException`, `IllegalArgumentException` and `PaymentGatewayException` are **caught and logged**, so the message is acked and gone.
  - Anything else is rethrown, rejected **with requeue**, and **redelivered forever** in a hot loop. That includes JSON parse failures, which are wrapped in a `RuntimeException` so the fatal-exception strategy doesn't recognise them, and `TooManyPaymentAttemptsException`.
  - Unknown routing key: warning logged, message acked and dropped.

## DB write vs. publish

`CreateAndAuthorizePaymentService.createAndAuthorize` is `@Transactional`. Inside it:

1. `CreatePendingPaymentService.create` runs a check-then-insert of the `PENDING` payment.
2. `AuthorizePaymentService.authorize` calls the gateway. On success it does `payment.authorize()`, then `save`, then **`eventBus.publish` inside the open transaction**.
3. On `PaymentGatewayException` it does `markAsFailed`, then **`publish(payment.failed)`**, then `save`, and then **rethrows**. The outer transaction therefore **rolls back** both the PENDING insert and the FAILED update, but the `payment.failed` message has already gone out.

`CapturePaymentService` and `VoidPaymentService` (REST-triggered) publish inside the transaction the same way. Consequences:

- Publish succeeds, then the commit fails: a phantom event describes state that doesn't exist.
- The commit succeeds, but the publish was dropped by the broker with no confirm: lost event.
- On a decline the DB has **no record of the attempt** (rolled back), while the invoice service is told it failed. This contradicts the README's "complete audit trail of all payment attempts" and makes `MAX_PAYMENT_ATTEMPTS` meaningless for declines.

## Retry / DLQ / idempotency

- **Retry:** absent, unless you count the infinite requeue loop.
- **DLQ:** absent.
- **Idempotency:** only a business check. `ensureNoActivePayment` throws `DuplicatePaymentException` if a PENDING/AUTHORIZED/CAPTURED payment exists for the invoice. It is check-then-insert with **no unique constraint**, so two concurrent deliveries can both pass it. It also doesn't cover a redelivered `invoice.retried` after a decline: each copy would create a new attempt and call the gateway again.

## Config

- `application.properties` maps env vars: `DB_*`, `RABBITMQ_HOST/PORT/USERNAME/PASSWORD` **and** `RABBITMQ_URL`. `spring.rabbitmq.addresses` wins over host/port.
- `spring-dotenv` loads `.env`.
- Exchange, queue and routing-key properties exist (`invoice.queue`, `payment.routing.*`, ...), but `RabbitMQConfig` reads the raw env names with `@Value("${INVOICE_EXCHANGE:...}")` defaults, and the listener hard-codes its queue name.
- `spring.profiles.active=${SPRING_PROFILE:dev}`. The only `PaymentGateway` bean is `@Profile("dev")`.

## Tests

`PaymentServiceApplicationTests.contextLoads` with Testcontainers (Postgres + RabbitMQ). There are no messaging-specific tests. The context also needs `DB_DRIVER` etc. resolvable, which in practice means a `.env` file must exist.

## Observations (not fixed in this phase)

1. **Routing key drift.** `.env.example` sets `INVOICE_RETRIED_ROUTING_KEY=invoice.retry`, but Node publishes `invoice.retried` and the consumer's switch expects `invoice.retried`. With the example env, retries are **unroutable and silently dropped**.
2. **Gateway declines roll back the payment row but still emit `payment.failed`** (see above).
3. **Poison messages loop forever:** malformed JSON and `TooManyPaymentAttemptsException`.
4. **Events are published inside DB transactions with no confirms:** classic dual-write.
5. **`ensureNoActivePayment` has no DB constraint behind it,** so it races.
6. **One exchange carries both services' events,** and the queue `invoice_events` has the same name as the exchange. The ownership is confusing.
7. **The invoice service's queue (`payment_events`) is declared here,** so the invoice service has a start-order dependency on this one.
8. **Dates are serialized as Jackson arrays and zone-less `LocalDateTime`.** The Node consumer depends on this exact shape.
9. **`AuthorizePaymentService.authorize` is `protected @Transactional`.** It works because CGLIB proxies intercept protected methods, but it's easy to break.
10. **`customerId` is never passed to the gateway** (`""`, there's a TODO).
11. **The gateway call happens inside the DB transaction.** A slow gateway holds a DB connection, and a crash after the gateway authorizes but before commit means a redelivery re-authorizes. The dummy gateway has no idempotency key.
12. **`spring.rabbitmq.host/port/username/password` and `addresses` are both set.** This is redundant and confusing.
