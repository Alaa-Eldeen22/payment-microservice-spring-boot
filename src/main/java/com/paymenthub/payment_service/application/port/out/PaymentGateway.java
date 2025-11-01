package com.paymenthub.payment_service.application.port.out;

import java.math.BigDecimal;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;

public interface PaymentGateway {

    String authorize(
            String paymentId,
            String customerId,
            String paymentMethodId,
            BigDecimal amount,
            String currency) throws PaymentGatewayException;

    void capture(String gatewayReferenceId) throws PaymentGatewayException;

    void voidAuthorization(String gatewayReferenceId) throws PaymentGatewayException;
}