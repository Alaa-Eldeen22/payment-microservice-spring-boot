package com.paymenthub.payment_service.application.port.out;

import java.time.LocalDateTime;
import java.util.Map;

public interface AuditPort {
    void auditPaymentEvent(String paymentId, String eventType, String userId,
            LocalDateTime timestamp, Map<String, Object> details);
}