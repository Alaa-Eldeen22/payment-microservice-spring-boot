# 💳 Payment Microservice

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2B%20DDD-blue.svg)](<https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)>)

> **A production-grade payment processing microservice demonstrating advanced software architecture patterns, domain-driven design, and event-driven communication.**

Built as a learning project to master enterprise-level software architecture, this microservice handles payment authorization, capture, and reconciliation flows while maintaining clean separation of concerns and following SOLID principles.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Domain Model](#-domain-model)
- [Getting Started](#-getting-started)
- [Messaging (RabbitMQ)](#-messaging-rabbitmq)
- [What I Learned](#-what-i-learned)
- [Future Enhancements](#-future-enhancements)

---

## 🎯 Overview

The Payment Service is a **domain-centric microservice** that handles payment processing in an invoicing system. It demonstrates:

- **Hexagonal Architecture (Ports & Adapters)** - Clean separation between business logic and infrastructure
- **Domain-Driven Design (DDD)** - Rich domain model with entities, value objects, and domain events
- **Event-Driven Architecture** - Asynchronous communication with other microservices via message brokers
- **CQRS Principles** - Separation of command and query responsibilities

### Business Context

In a typical invoicing system flow:

1. **Invoice Service** creates an invoice → publishes `InvoiceCreatedEvent`
2. **Payment Service** receives event → automatically initiates payment authorization
3. On success: Payment is authorized → can be captured later (funds transfer)
4. On failure: Payment is marked as failed → customer can retry with a different payment method

This simulates real-world payment flows used by companies like Stripe, PayPal, and Square.

---

## 🏗 Architecture

### Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Inbound Adapters                        │   │
│  │  • REST API (PaymentController)                      │   │
│  │  • Message Consumer (RabbitMQ)                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ▲                                  │
│                           │                                  │
│  ┌────────────────────────┴──────────────────────────────┐  │
│  │               Application Layer                       │  │
│  │  • Use Cases (CreatePayment, AuthorizePayment...)    │  │
│  │  • Commands & DTOs                                    │  │
│  │  • Ports (Interfaces)                                 │  │
│  └────────────────────────┬──────────────────────────────┘  │
│                           │                                  │
│                           ▼                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                Domain Layer (Pure)                    │   │
│  │  • Payment Entity (Aggregate Root)                   │   │
│  │  • Value Objects (Money, InvoiceId)                  │   │
│  │  • Domain Events                                      │   │
│  │  • Business Rules & Invariants                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ▲                                  │
│                           │                                  │
│  ┌────────────────────────┴──────────────────────────────┐  │
│  │             Outbound Adapters                         │  │
│  │  • Database (JPA Repository)                          │  │
│  │  • Payment Gateway (Stripe-like)                      │  │
│  │  • Event Publisher (RabbitMQ)                         │  │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Why This Architecture?

**Traditional Layered Architecture:**

```
Controller → Service → Repository → Database
```

❌ Problem: Business logic leaks into all layers, hard to test, tight coupling

**Hexagonal Architecture:**

```
Adapters → Ports → Domain (Core) ← Ports ← Adapters
```

✅ Solution: Domain is isolated, testable, and independent of frameworks/databases

---

## ✨ Key Features

### 1. **Automatic Payment Processing**

- Consumes `InvoiceCreatedEvent` from message broker
- Automatically initiates payment authorization
- Idempotent consumption: dedup on the message `idempotencyKey`, at-least-once delivery via a transactional outbox

### 2. **Manual Payment Retry**

- Customers can retry failed payments with new payment methods
- Validates business rules (only one active payment per invoice)
- Complete audit trail of all payment attempts

### 3. **Two-Phase Payment Flow**

- **Authorization**: Reserve funds on customer's payment method (7-day hold)
- **Capture**: Actually transfer funds (can be partial)
- Supports **Void** (cancel authorization before capture)

### 4. **Rich Domain Model**

- Pure domain entities with no infrastructure dependencies
- Value Objects enforce invariants (Money, InvoiceId, PaymentMethodId)
- Domain Events for cross-service communication
- State machine with validated transitions

### 5. **Event-Driven Communication**

- Publishes domain events: `PaymentAuthorized`, `PaymentFailed`, `PaymentCaptured`
- Loose coupling with other microservices
- Supports eventual consistency

### 6. **Payment Gateway Abstraction**

- Generic interface for payment providers (Stripe, PayPal, etc.)
- Easy to swap implementations
- Demo implementation included

---

## 🛠 Technology Stack

### Core Framework

- **Java 17** - Modern Java features (Records, Pattern Matching)
- **Spring Boot 3.2+** - Application framework
- **Spring Data JPA** - Database access
- **Hibernate** - ORM

### Messaging & Events

- **RabbitMQ** - Message broker for event-driven architecture
- **AMQP** - Messaging protocol

### Database

- **PostgreSQL** - Relational database

### Architecture & Design

- **Hexagonal Architecture** - Ports & Adapters pattern
- **Domain-Driven Design (DDD)** - Tactical patterns
- **CQRS** - Command Query Responsibility Segregation (principles)

### Tools & Libraries

- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

---

## 📁 Project Structure

```
payment_service/
├── 📂 domain/                          # Pure business logic (no dependencies)
│   ├── entity/                         # Aggregate roots
│   │   └── Payment.java                # Core entity with business rules
│   ├── valueobject/                    # Immutable value objects
│   │   ├── Money.java                  # Currency-aware amount
│   │   ├── InvoiceId.java              # Type-safe ID
│   │   └── PaymentMethodId.java        # Payment method reference
│   ├── events/                         # Domain events
│   │   ├── PaymentAuthorizedEvent.java
│   │   ├── PaymentCapturedEvent.java
│   │   └── PaymentFailedEvent.java
│   ├── repository/                     # Repository interfaces (ports)
│   │   └── PaymentRepository.java
│   ├── service/                        # Domain services
│   │   └── PaymentAuthorizationDomainService.java
│   └── exception/                      # Domain exceptions
│       ├── IllegalPaymentStateException.java
│       └── InsufficientAuthorizationException.java
│
├── 📂 application/                     # Use cases & orchestration
│   ├── port/                           # Ports (interfaces)
│   │   ├── in/                         # Inbound ports (driving side)
│   │   │   ├── command/                # Command DTOs
│   │   │   │   ├── CreatePaymentCommand.java
│   │   │   │   └── AuthorizePaymentCommand.java
│   │   │   └── usecase/                # Use case interfaces
│   │   │       ├── CreatePaymentUseCase.java
│   │   │       └── AuthorizePaymentUseCase.java
│   │   └── out/                        # Outbound ports (driven side)
│   │       ├── EventBus.java           # Event publishing
│   │       └── PaymentGateway.java     # Payment provider
│   ├── service/                        # Use case implementations
│   │   ├── CreateAndAuthorizePaymentService.java
│   │   ├── CapturePaymentService.java
│   │   └── VoidPaymentService.java
│   └── dto/                            # Data transfer objects
│       └── result/
│           └── PaymentResult.java
│
└── 📂 infrastructure/                  # External concerns & adapters
    ├── adapter/
    │   ├── in/                         # Inbound adapters
    │   │   ├── rest/                   # REST API
    │   │   │   └── PaymentController.java
    │   │   └── messaging/              # Message consumers
    │   │       └── consumer/
    │   │           └── RabbitInvoiceEventConsumer.java
    │   └── out/                        # Outbound adapters
    │       ├── persistence/            # Database
    │       │   ├── entity/
    │       │   │   └── PaymentEntity.java  # JPA entity
    │       │   ├── mapper/
    │       │   │   └── PaymentMapper.java  # Domain ↔ JPA mapping
    │       │   └── repository/
    │       │       └── JpaPaymentRepository.java
    │       ├── gateway/                # External services
    │       │   └── DummyPaymentGateway.java
    │       └── messaging/              # Event publishing
    │           └── RabbitMQEventBus.java
    └── config/                         # Spring configuration
        └── RabbitMQConfig.java
```

### Layer Responsibilities

| Layer              | Purpose                         | Dependencies         |
| ------------------ | ------------------------------- | -------------------- |
| **Domain**         | Business logic, entities, rules | **NONE** (pure Java) |
| **Application**    | Use cases, orchestration        | Domain only          |
| **Infrastructure** | Frameworks, databases, APIs     | Application + Domain |

---

## 🎨 Domain Model

### Payment Aggregate

```java
Payment (Aggregate Root)
├── id: String
├── invoiceId: InvoiceId (Value Object)
├── paymentMethodId: PaymentMethodId (Value Object)
├── requestedAmount: Money (Value Object)
├── authorizedAmount: Money (Value Object)
├── capturedAmount: Money (Value Object)
├── status: PaymentStatus (Enum)
│   ├── PENDING
│   ├── AUTHORIZED
│   ├── PARTIALLY_CAPTURED
│   ├── CAPTURED
│   ├── FAILED
│   ├── VOIDED
│   └── REFUNDED
├── paymentGatewayReferenceId: String
├── timestamps: LocalDateTime
└── domainEvents: List<DomainEvent>
```

### State Machine

```
PENDING ──authorize()──> AUTHORIZED ──capture()──> CAPTURED
   │                          │
   │                          └──voidAuthorization()──> VOIDED
   │
   └──markAsFailed()──> FAILED (can retry)
```

### Domain Events

Events are published when state changes occur:

```java
PaymentAuthorizedEvent    // Payment authorized with gateway
PaymentCapturedEvent      // Funds captured
PaymentFailedEvent        // Payment failed (card declined, etc.)
PaymentVoidedEvent        // Authorization cancelled
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- RabbitMQ (Docker recommended)
- PostgreSQL/MySQL

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/payment-service.git
cd payment-service
```

### 2. Start Dependencies (Docker)

```bash
# Start RabbitMQ
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=payment_db \
  -e POSTGRES_USER=payment_user \
  -e POSTGRES_PASSWORD=payment_pass \
  -p 5432:5432 \
  postgres:15
```

### 3. Configure Application

Copy `.env.example` to `.env` (loaded by spring-dotenv). Every value has a default in `application.properties`:

```env
DB_URL=jdbc:postgresql://localhost:5432/payment_service_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
SPRING_PROFILE=dev                      # dev = DummyPaymentGateway (paymentMethodId "fail" declines)

# exchange/queue names are fixed by the messaging contract, not configurable
RABBITMQ_URL=amqp://guest:guest@localhost:5672
RABBITMQ_PREFETCH=10
RABBITMQ_PUBLISH_TIMEOUT_MS=10000
OUTBOX_POLL_INTERVAL_MS=1000
OUTBOX_BATCH_SIZE=50
# LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs # JSON logs incl. correlation/message ids
```

### 4. Build & Run

```bash
./mvnw clean package          # runs unit + Testcontainers tests (needs Docker)
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
# or
./mvnw spring-boot:run
```

---

## 📨 Messaging (RabbitMQ)

Messaging follows the shared **[messaging contract](https://github.com/Alaa-Eldeen22/invoice-microservice-nestjs/blob/main/docs/MESSAGING-CONTRACT.md)**. It is kept in the invoice repo, and in the local `invoicing/` workspace it's `../MESSAGING-CONTRACT.md`. Both services implement the same envelope, topology, retry and DLQ semantics. In short:

- **Transactional outbox.** The `EventBus` port is implemented by `OutboxEventBus` (`@Transactional(propagation = MANDATORY)`). Events are rows in `outbox_messages`, written in the same transaction as the payment. `OutboxRelay` (`@Scheduled`, `FOR UPDATE SKIP LOCKED`) publishes with **correlated publisher confirms + mandatory returns** and marks a row sent only after a confirmed, routed publish.
- **Consumer** `InvoiceEventsListener` on `payment-service.invoice-events` (quorum queue), `AckMode.MANUAL`. In one transaction it inserts the dedup row (`processed_messages`, `INSERT … ON CONFLICT DO NOTHING` on the envelope `idempotencyKey`) and runs the use case; it acks only after commit. Duplicates are skipped.
- **Retries:** 3, through per-step TTL queues (5 s → 30 s → 2 min), then **DLQ** `payment-service.invoice-events.dlq` with `x-dlq-*` metadata. Malformed, unknown-type and unsupported-version messages go straight to the DLQ. `TooManyPaymentAttemptsException` is non-retryable. `DuplicatePaymentException` is a no-op ack.
- A **gateway decline** commits the FAILED payment and publishes `payment.failed`. It used to roll both back.
- **Observability:** one structured log line per step (`outbox.enqueued`, `rmq.publish.ok`, `rmq.consume.dead_lettered`, …). `cid`/`mid` from the MDC appear on every line. REST takes and returns `X-Correlation-Id`.

| Doc | What |
|---|---|
| [MESSAGING-CONTRACT.md](https://github.com/Alaa-Eldeen22/invoice-microservice-nestjs/blob/main/docs/MESSAGING-CONTRACT.md) | envelope, topology, retry/DLQ, idempotency, versioning |
| [docs/messaging-runbook.md](docs/messaging-runbook.md) | operators: inspect and replay the DLQ, failure modes in logs |
| [docs/messaging-notes.md](docs/messaging-notes.md) | how messaging worked *before* this design (historical) |
| invoice repo `docs/messaging-verification.md` | cross-service verification run |

Tests: `MessageProcessorTest` (unit) and `MessagingFlowIntegrationTest` (Testcontainers RabbitMQ + Postgres: outbox, duplicates, decline, retry→DLQ, poison, unroutable).

## 💡 What I Learned

### Technical Skills

#### Software Architecture

- **Hexagonal Architecture**: How to properly separate business logic from infrastructure
- **Domain-Driven Design**: Designing rich domain models with entities, value objects, and aggregates
- **CQRS Principles**: Separating command and query responsibilities
- **Event-Driven Architecture**: Asynchronous communication between microservices

#### Design Principles

- **SOLID Principles**: Especially Dependency Inversion (depending on abstractions)
- **Clean Code**: Writing maintainable, testable code
- **Separation of Concerns**: Each layer has a single, well-defined responsibility

#### Technical Implementation

- **Spring Boot**: Advanced features (events, transactions, messaging)
- **JPA/Hibernate**: ORM, entity mapping, repository pattern
- **RabbitMQ**: Message-driven architecture, event publishing/consuming
- **Testing**: Unit tests with Mockito, integration tests

### Business Domain Knowledge

#### Payment Processing

- **Authorization vs Capture**: Two-phase payment flow (reserve funds, then transfer)
- **Payment States**: Understanding payment lifecycle and state transitions
- **Idempotency**: Preventing duplicate charges
- **Reconciliation**: Tracking payment attempts and matching to invoices

#### Real-World Scenarios

- **Failed Payments**: Handling card declines, insufficient funds
- **Payment Retries**: Allowing customers to retry with different payment methods
- **Partial Authorizations**: What happens when gateway authorizes less than requested
- **Audit Trail**: Maintaining complete history for compliance

### Problem-Solving Approaches

1. **Start with Domain**: Model business rules first, infrastructure later
2. **Make Illegal States Impossible**: Use type system and value objects to enforce invariants
3. **Think in Events**: Communication between services via domain events
4. **Test Behavior, Not Implementation**: Focus on business outcomes

---

## 🔮 Future Enhancements

- [ ] Add comprehensive unit and integration tests
- [ ] Implement real payment gateway integration (currently using dummy gateway)
- [ ] Metrics for the messaging pipeline (log events are counter-ready; no metrics stack yet)
- [ ] Pass a gateway idempotency key once a real gateway is integrated
- [ ] Implement full/partial refunds
- [ ] Handle async payment gateway callbacks useing webhooks

---

## 🤝 Contributing

This is a learning project, but suggestions and feedback are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<div align="center">

**⭐ If you found this project helpful, please consider giving it a star! ⭐**

Made with 💙 and lots of ☕ for learning enterprise software architecture.
**Part of a microservices ecosystem:**

- **Payment Service** (This repo) - Java/Spring Boot
- **Invoice Service** - TypeScript/NestJS you can find the repo [HERE](https://github.com/Alaa-Eldeen22/invoice-microservice-nestjs)

</div>
