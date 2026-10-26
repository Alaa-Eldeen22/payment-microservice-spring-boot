package com.paymenthub.payment_service.infrastructure.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * One line per messaging step with a stable event name (contract §9):
 * {@code rmq.consume.ok messageId=... correlationId=...}. Same names and keys
 * as the invoice service so a message can be grepped across both.
 */
public final class MessagingLog {

    private MessagingLog() {
    }

    public static Map<String, Object> fields(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }

    public static void log(Logger logger, Level level, String event, Map<String, Object> fields) {
        String kv = fields.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
        logger.atLevel(level).log("{} {}", event, kv);
    }

    public static String describe(Throwable err) {
        return describe(err, 1000);
    }

    public static String describe(Throwable err, int max) {
        String text = err.getClass().getSimpleName() + ": " + err.getMessage();
        return text.length() > max ? text.substring(0, max) : text;
    }
}
