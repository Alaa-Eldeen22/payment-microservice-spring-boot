package com.paymenthub.payment_service.infrastructure.messaging;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.MDC;

/**
 * Correlation data of the flow the current thread is working on (an HTTP
 * request or a consumed message). Events written to the outbox inherit it,
 * and it's mirrored into the MDC so every log line carries it.
 */
public final class MessageContext {

    public record Data(String correlationId, String causationId) {
    }

    private static final ThreadLocal<Data> CURRENT = new ThreadLocal<>();

    private MessageContext() {
    }

    public static Optional<Data> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static <T> T run(Data data, Supplier<T> work) {
        Data previous = CURRENT.get();
        String previousMdc = MDC.get("correlationId");
        CURRENT.set(data);
        MDC.put("correlationId", data.correlationId());
        try {
            return work.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
            if (previousMdc == null) {
                MDC.remove("correlationId");
            } else {
                MDC.put("correlationId", previousMdc);
            }
        }
    }
}
