package com.paymenthub.payment_service.infrastructure.messaging;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paymenthub.payment_service.infrastructure.messaging.MessagingExceptions.MalformedMessageException;

/**
 * Wire format of every message, see MESSAGING-CONTRACT.md §2. The payload is
 * kept as a JSON tree; handlers validate it for their eventType/version.
 */
public record MessageEnvelope(
        String messageId,
        String idempotencyKey,
        String eventType,
        int schemaVersion,
        String occurredAt,
        String correlationId,
        String causationId,
        String producer,
        JsonNode payload) {

    private static final Pattern UUID_RE = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public static MessageEnvelope parse(byte[] body, ObjectMapper mapper) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw new MalformedMessageException("body is not valid JSON: " + e.getMessage());
        }
        if (root == null || !root.isObject()) {
            throw new MalformedMessageException("body must be a JSON object");
        }

        String messageId = requireText(root, "messageId", 1000);
        if (!UUID_RE.matcher(messageId).matches()) {
            throw new MalformedMessageException("envelope.messageId must be a UUID");
        }
        String occurredAt = requireText(root, "occurredAt", 100);
        requireTimestamp(occurredAt, "envelope.occurredAt");

        JsonNode version = root.get("schemaVersion");
        if (version == null || !version.isIntegralNumber() || version.asInt() < 1) {
            throw new MalformedMessageException("envelope.schemaVersion must be a positive integer");
        }
        JsonNode causation = root.get("causationId");
        if (causation != null && !causation.isNull() && !causation.isTextual()) {
            throw new MalformedMessageException("envelope.causationId must be a string or null");
        }
        JsonNode payload = root.get("payload");
        if (payload == null || !payload.isObject()) {
            throw new MalformedMessageException("envelope.payload must be an object");
        }

        return new MessageEnvelope(
                messageId,
                requireText(root, "idempotencyKey", 200),
                requireText(root, "eventType", 100),
                version.asInt(),
                occurredAt,
                requireText(root, "correlationId", 100),
                causation == null || causation.isNull() ? null : causation.asText(),
                requireText(root, "producer", 100),
                payload);
    }

    /** Builds a new outgoing envelope with a fresh messageId. */
    public static MessageEnvelope create(String eventType, String idempotencyKey, Instant occurredAt,
            String correlationId, String causationId, ObjectNode payload) {
        return new MessageEnvelope(
                UUID.randomUUID().toString(),
                idempotencyKey,
                eventType,
                1,
                occurredAt.toString(),
                correlationId,
                causationId,
                MessagingConstants.SERVICE_NAME,
                payload);
    }

    public String toJson(ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize envelope " + messageId, e);
        }
    }

    public static String requireText(JsonNode node, String field, int max) {
        return requireText(node, "envelope." + field, field, max);
    }

    public static String requireText(JsonNode node, String label, String field, int max) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isEmpty() || value.asText().length() > max) {
            throw new MalformedMessageException(label + " must be a non-empty string");
        }
        return value.asText();
    }

    public static Instant requireTimestamp(String value, String field) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new MalformedMessageException(field + " must be an RFC 3339 UTC timestamp");
        }
    }
}
