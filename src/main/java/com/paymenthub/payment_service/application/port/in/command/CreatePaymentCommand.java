package com.paymenthub.payment_service.application.port.in.command;

import java.math.BigDecimal;

public record CreatePaymentCommand(
                String invoiceId,
                BigDecimal amount,
                String currency,
                String paymentMethodId) {
}
