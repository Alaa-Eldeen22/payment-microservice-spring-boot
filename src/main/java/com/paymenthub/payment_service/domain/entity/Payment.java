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
import com.paymenthub.payment_service.domain.exception.InsufficientAuthorizationException;
import com.paymenthub.payment_service.domain.exception.PaymentExpiredException;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;

public class Payment {

    private String id;
    private InvoiceId invoiceId;
    private Money authorizedAmount;
    private Money capturedAmount;
    private PaymentStatus status;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    // Protected constructor
    private Payment() {
    }

    // Factory method
    public static Payment createPendingPayment(InvoiceId invoiceId, Money amount) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID().toString();
        payment.invoiceId = invoiceId;
        payment.authorizedAmount = amount;
        payment.capturedAmount = new Money(BigDecimal.ZERO, amount.getCurrencyCode());
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = LocalDateTime.now();
        return payment;
    }

    // Business methods
    public void authorize(String stripePaymentIntentId) {
        if (!status.canBeAuthorized()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot authorize payment in status: %s", status));
        }

        this.stripePaymentIntentId = stripePaymentIntentId;
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
        this.expiresAt = authorizedAt.plusDays(7); // Stripe default

        addDomainEvent(new PaymentAuthorizedEvent(
                this.id,
                this.invoiceId.getValue(),
                this.authorizedAmount.getAmount(),
                this.authorizedAmount.getCurrencyCode()));
    }

    public void capture(Money captureAmount) {
        validateCaptureOperation(captureAmount);

        Money newTotalCaptured = this.capturedAmount.add(captureAmount);
        this.capturedAmount = newTotalCaptured;
        this.capturedAt = LocalDateTime.now();

        // Update status based on capture amount
        if (newTotalCaptured.getAmount().equals(authorizedAmount.getAmount())) {
            this.status = PaymentStatus.CAPTURED;
        } else {
            this.status = PaymentStatus.PARTIALLY_CAPTURED;
        }

        addDomainEvent(new PaymentCapturedEvent(
                this.id,
                captureAmount.getAmount(),
                newTotalCaptured.getAmount()));
    }

    public void voidAuthorization() {
        if (!status.canBeVoided()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot void payment in status: %s", status));
        }

        this.status = PaymentStatus.VOIDED;
        addDomainEvent(new PaymentVoidedEvent(this.id));
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        addDomainEvent(new PaymentFailedEvent(this.id, reason));
    }

    public Money getRemainingAuthorizationAmount() {
        return authorizedAmount.subtract(capturedAmount);
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

    private void validateCaptureOperation(Money captureAmount) {
        if (!status.canBeCaptured()) {
            throw new IllegalPaymentStateException(
                    String.format("Cannot capture payment in status: %s", status));
        }

        if (isAuthorizationExpired()) {
            throw new PaymentExpiredException("Cannot capture expired authorization");
        }

        Money remainingAmount = getRemainingAuthorizationAmount();
        if (captureAmount.isGreaterThan(remainingAmount)) {
            throw new InsufficientAuthorizationException(
                    String.format("Capture amount %s exceeds remaining authorization %s",
                            captureAmount.getAmount(), remainingAmount.getAmount()));
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
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

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
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
}