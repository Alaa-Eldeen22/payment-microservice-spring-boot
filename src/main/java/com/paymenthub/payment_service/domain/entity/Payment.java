package com.paymenthub.payment_service.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.paymenthub.payment_service.domain.enums.PaymentStatus;
import com.paymenthub.payment_service.domain.events.DomainEvent;
import com.paymenthub.payment_service.domain.events.PaymentAuthorizedEvent;
import com.paymenthub.payment_service.domain.events.PaymentVoidedEvent;
import com.paymenthub.payment_service.domain.events.PaymentCapturedEvent;
import com.paymenthub.payment_service.domain.events.PaymentFailedEvent;
import com.paymenthub.payment_service.domain.exception.IllegalPaymentStateException;
import com.paymenthub.payment_service.domain.exception.PaymentExpiredException;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.PaymentMethodId;
import com.paymenthub.payment_service.domain.valueobject.Money;

public class Payment {

    private String id;
    private InvoiceId invoiceId;
    private PaymentMethodId paymentMethodId;
    private Money requestedAmount;
    private Money authorizedAmount;
    private Money capturedAmount;
    private PaymentStatus status;
    private String paymentGatewayReferenceId;
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    private static final long AUTHORIZATION_EXPIRY_DAYS = 7;

    private Payment() {
    }

    public static Payment createPendingPayment(InvoiceId invoiceId, PaymentMethodId paymentMethodId, Money amount) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID().toString();
        payment.invoiceId = invoiceId;
        payment.requestedAmount = amount;
        payment.authorizedAmount = new Money(BigDecimal.ZERO, amount.getCurrencyCode());
        payment.capturedAmount = new Money(BigDecimal.ZERO, amount.getCurrencyCode());
        payment.paymentMethodId = paymentMethodId;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }

    public void authorize(String paymentGatewayReferenceId) {
        if (!status.canBeAuthorized()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot authorize payment in status: %s", status));
        }

        this.authorizedAmount = this.requestedAmount;
        this.paymentGatewayReferenceId = paymentGatewayReferenceId;
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
        this.expiresAt = authorizedAt.plusDays(AUTHORIZATION_EXPIRY_DAYS);

        addDomainEvent(new PaymentAuthorizedEvent(
                this.id,
                this.invoiceId.getValue()));
    }

    public void capture() {
        validateCaptureOperation();

        this.capturedAmount = this.authorizedAmount;
        this.capturedAt = LocalDateTime.now();

        this.status = PaymentStatus.CAPTURED;

        addDomainEvent(new PaymentCapturedEvent(
                this.id,
                this.invoiceId.getValue(),
                this.capturedAt));
    }

    public void voidAuthorization() {
        if (!status.canBeVoided()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot void payment in status: %s", status));
        }

        this.status = PaymentStatus.VOIDED;
        addDomainEvent(new PaymentVoidedEvent(this.id, this.invoiceId.getValue()));
    }

    public void markAsFailed(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot mark payment as failed in status: %s", status));
        }

        this.status = PaymentStatus.FAILED;
        addDomainEvent(new PaymentFailedEvent(
                this.id,
                this.invoiceId.getValue(),
                reason));
    }

    public boolean isAuthorizationExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // Domain event management
    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private void validateCaptureOperation() {
        if (!status.canBeCaptured()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot capture payment in status: %s", status));
        }

        if (isAuthorizationExpired()) {
            throw new PaymentExpiredException("Cannot capture expired authorization");
        }

    }

    // Getters
    public String getId() {
        return id;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public PaymentMethodId getPaymentMethodId() {
        return paymentMethodId;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }

    public Money getAuthorizedAmount() {
        return authorizedAmount;
    }

    public Money getCapturedAmount() {
        return capturedAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentGatewayReferenceId() {
        return paymentGatewayReferenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public static class Builder {
        private String id;
        private InvoiceId invoiceId;
        private PaymentMethodId paymentMethodId;
        private Money requestedAmount;
        private Money authorizedAmount;
        private Money capturedAmount;
        private PaymentStatus status;
        private String paymentGatewayReferenceId;
        private LocalDateTime createdAt;
        private LocalDateTime authorizedAt;
        private LocalDateTime capturedAt;
        private LocalDateTime expiresAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder invoiceId(InvoiceId invoiceId) {
            this.invoiceId = invoiceId;
            return this;
        }

        public Builder paymentMethodId(PaymentMethodId paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
            return this;
        }

        public Builder authorizedAmount(Money authorizedAmount) {
            this.authorizedAmount = authorizedAmount;
            return this;
        }

        public Builder requestedAmount(Money requestedAmount) {
            this.requestedAmount = requestedAmount;
            return this;
        }

        public Builder capturedAmount(Money capturedAmount) {
            this.capturedAmount = capturedAmount;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder paymentGatewayReferenceId(String paymentGatewayReferenceId) {
            this.paymentGatewayReferenceId = paymentGatewayReferenceId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder authorizedAt(LocalDateTime authorizedAt) {
            this.authorizedAt = authorizedAt;
            return this;
        }

        public Builder capturedAt(LocalDateTime capturedAt) {
            this.capturedAt = capturedAt;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Payment build() {
            Payment payment = new Payment();
            payment.id = this.id;
            payment.invoiceId = this.invoiceId;
            payment.paymentMethodId = this.paymentMethodId;
            payment.requestedAmount = this.requestedAmount;
            payment.authorizedAmount = this.authorizedAmount;
            payment.capturedAmount = this.capturedAmount != null ? this.capturedAmount
                    : new Money(BigDecimal.ZERO, this.authorizedAmount.getCurrencyCode());
            payment.status = this.status;
            payment.paymentGatewayReferenceId = this.paymentGatewayReferenceId;
            payment.createdAt = this.createdAt;
            payment.authorizedAt = this.authorizedAt;
            payment.capturedAt = this.capturedAt;
            payment.expiresAt = this.expiresAt;
            payment.domainEvents = new ArrayList<>();
            return payment;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}