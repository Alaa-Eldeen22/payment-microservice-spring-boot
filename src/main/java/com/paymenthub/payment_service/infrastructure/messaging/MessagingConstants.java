package com.paymenthub.payment_service.infrastructure.messaging;

/**
 * Names and limits from MESSAGING-CONTRACT.md (§3, §10). The invoice service
 * declares the same objects - change both sides together.
 */
public final class MessagingConstants {

    private MessagingConstants() {
    }

    public static final String SERVICE_NAME = "payment-service";

    public static final String INVOICE_EVENTS_EXCHANGE = "invoice.events";
    public static final String PAYMENT_EVENTS_EXCHANGE = "payment.events";

    /** Queue this service consumes invoice events from. */
    public static final String INVOICE_EVENTS_QUEUE = "payment-service.invoice-events";

    public static final String INVOICE_CREATED = "invoice.created";
    public static final String INVOICE_RETRIED = "invoice.retried";
    public static final String PAYMENT_AUTHORIZED = "payment.authorized";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String PAYMENT_VOIDED = "payment.voided";

    public static final int MAX_RETRIES = 3;
    public static final int MAIN_QUEUE_DELIVERY_LIMIT = 10;

    public static String retryExchange(String queue) {
        return queue + ".retry";
    }

    public static String retryQueue(String queue, int step) {
        return queue + ".retry." + step;
    }

    public static String deadLetterExchange(String queue) {
        return queue + ".dlx";
    }

    public static String deadLetterQueue(String queue) {
        return queue + ".dlq";
    }

    public static final class Headers {
        private Headers() {
        }

        public static final String SCHEMA_VERSION = "x-schema-version";
        public static final String RETRY_COUNT = "x-retry-count";
        public static final String ORIGINAL_EXCHANGE = "x-original-exchange";
        public static final String ORIGINAL_ROUTING_KEY = "x-original-routing-key";
        public static final String FIRST_FAILED_AT = "x-first-failed-at";
        public static final String LAST_ERROR = "x-last-error";
        public static final String DLQ_REASON = "x-dlq-reason";
        public static final String DLQ_ERROR = "x-dlq-error";
        public static final String DLQ_ATTEMPTS = "x-dlq-attempts";
        public static final String DLQ_QUEUE = "x-dlq-queue";
        public static final String DLQ_CONSUMER = "x-dlq-consumer";
        public static final String DLQ_AT = "x-dlq-at";
    }
}
