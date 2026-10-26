package com.paymenthub.payment_service.infrastructure.messaging;

/** Failure types the consumer/publisher pipeline classifies on (contract §4.2). */
public final class MessagingExceptions {

    private MessagingExceptions() {
    }

    /** Body / envelope / payload doesn't match the contract. Straight to DLQ. */
    public static class MalformedMessageException extends RuntimeException {
        public MalformedMessageException(String message) {
            super(message);
        }
    }

    public static class UnknownEventTypeException extends RuntimeException {
        public UnknownEventTypeException(String message) {
            super(message);
        }
    }

    public static class UnsupportedSchemaVersionException extends RuntimeException {
        public UnsupportedSchemaVersionException(String message) {
            super(message);
        }
    }

    /** Thrown by handlers for failures retrying can't fix. Skips the retry ladder. */
    public static class NonRetryableMessageException extends RuntimeException {
        public NonRetryableMessageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Broker could not route a mandatory publish. */
    public static class UnroutableMessageException extends RuntimeException {
        public UnroutableMessageException(String message) {
            super(message);
        }
    }

    /** Broker nacked, timed out or the connection failed before the confirm. */
    public static class PublishFailedException extends RuntimeException {
        public PublishFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
