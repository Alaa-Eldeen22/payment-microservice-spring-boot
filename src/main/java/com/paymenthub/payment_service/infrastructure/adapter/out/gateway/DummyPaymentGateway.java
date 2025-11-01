package com.paymenthub.payment_service.infrastructure.adapter.out.gateway;

import com.paymenthub.payment_service.application.port.out.PaymentGateway;
import com.paymenthub.payment_service.application.exception.PaymentGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Primary
@Profile("dev")
@Slf4j
public class DummyPaymentGateway implements PaymentGateway {

    @Override
    public String authorize(
            String paymentId,
            String customerId,
            String paymentMethodId,
            BigDecimal amount,
            String currency) throws PaymentGatewayException {
        log.info("Dummy authorize called: paymentId={}, customerId={}, method={}, amount={}, currency={}",
                paymentId, customerId, paymentMethodId, amount, currency);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentGatewayException("Invalid amount for authorization", "INVALID_AMOUNT",
                    "amount must be > 0");
        }

        // test hooks: use paymentMethodId "fail" to simulate decline
        if ("fail".equalsIgnoreCase(paymentMethodId)) {
            throw new PaymentGatewayException("Simulated card decline", "SIM_DECLINE",
                    "card declined by dummy gateway");
        }

        String intentId = "dummy_pi_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Dummy authorization succeeded, intentId={}", intentId);
        return intentId;
    }

    @Override
    public void capture(String gatewayReferenceId) throws PaymentGatewayException {
        log.info("Dummy capture called: gatewayReferenceId={}, amount={}", gatewayReferenceId);

        if (gatewayReferenceId == null || gatewayReferenceId.isBlank()) {
            throw new PaymentGatewayException("Invalid gatewayReferenceId for capture", "INVALID_ID",
                    "missing gateway reference id");
        }

        if (gatewayReferenceId.startsWith("fail")) {
            throw new PaymentGatewayException("Simulated capture failure", "SIM_CAPTURE_FAILED",
                    "capture failed in dummy gateway");
        }

        // no-op (simulated success)
        log.info("Dummy capture completed for {}", gatewayReferenceId);
    }

    @Override
    public void voidAuthorization(String gatewayReferenceId) throws PaymentGatewayException {
        log.info("Dummy void called: gatewayReferenceId={}", gatewayReferenceId);

        if (gatewayReferenceId == null || gatewayReferenceId.isBlank()) {
            throw new PaymentGatewayException("Invalid gatewayReferenceId for void", "INVALID_ID",
                    "missing gateway reference id");
        }

        if (gatewayReferenceId.startsWith("fail")) {
            throw new PaymentGatewayException("Simulated void failure", "SIM_VOID_FAILED",
                    "void failed in dummy gateway");
        }

        // no-op (simulated success)
        log.info("Dummy void completed for {}", gatewayReferenceId);
    }
}