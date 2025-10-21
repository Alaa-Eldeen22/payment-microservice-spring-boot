# Payment Microservice

A Spring Boot microservice that handles payment processing for invoices as part of a larger invoicing system. Built with a hexagonal architecture (ports and adapters) pattern.

## Features

- Payment lifecycle management (create, authorize, capture, void)
- Event-driven integration with invoice service via RabbitMQ
- Domain-driven design with rich domain model
- Supports partial captures and payment status tracking
- Built-in validation and error handling
- Testcontainers for integration testing

## Tech Stack

- Java 17
- Spring Boot 3.5.3
- Spring Data JPA
- PostgreSQL
- RabbitMQ
- Maven
- Testcontainers
- Lombok

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/paymenthub/payment_service/
│   │       ├── application/          # Application services & DTOs
│   │       ├── domain/              # Domain model & business logic
│   │       └── infrastructure/      # External integrations
│   └── resources/
│       └── application.properties   # Configuration
└── test/
    └── java/                       # Test cases
```

## Getting Started

1. Clone the repository
2. Copy `.env.example` to `.env` and configure your environment variables
3. Start PostgreSQL and RabbitMQ (or use provided Testcontainers)
4. Run the application:

```bash
./mvnw spring-boot:run
```

## Configuration

Key configuration in `application.properties`:

- Database connection settings
- RabbitMQ connection and queue settings
- Invoice event handling configuration

## Testing

The project uses Testcontainers for integration testing. Run tests with:

```bash
./mvnw test
```

## Domain Model

The core domain model is centered around the [`Payment`](src/main/java/com/paymenthub/payment_service/domain/entity/Payment.java) entity which implements the payment lifecycle:

- PENDING → AUTHORIZED → CAPTURED
- Support for partial captures
- Void and refund operations
- Rich validation rules

## Event Handling

The service listens for [`InvoiceCreatedEvent`](src/main/java/com/paymenthub/payment_service/infrastructure/adapter/in/messaging/event/InvoiceCreatedEvent.java) messages from RabbitMQ and automatically creates corresponding payment records.