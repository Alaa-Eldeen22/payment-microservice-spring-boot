// Domain Events
package com.yourcompany.payment.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public abstract class DomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredOn = LocalDateTime.now();
}

@Getter
public class PaymentAuthorizedEvent extends DomainEvent {
    private final String paymentId;
    private final String invoiceId;
    private final BigDecimal amount;
    private final String currency;
    
    public PaymentAuthorizedEvent(String paymentId, String invoiceId, BigDecimal amount, String currency) {
        super();
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.currency = currency;
    }
}

@Getter
public class PaymentCapturedEvent extends DomainEvent {
    private final String paymentId;
    private final BigDecimal capturedAmount;
    private final BigDecimal totalCapturedAmount;
    
    public PaymentCapturedEvent(String paymentId, BigDecimal capturedAmount, BigDecimal totalCapturedAmount) {
        super();
        this.paymentId = paymentId;
        this.capturedAmount = capturedAmount;
        this.totalCapturedAmount = totalCapturedAmount;
    }
}

@Getter
public class PaymentFailedEvent extends DomainEvent {
    private final String paymentId;
    private final String reason;
    
    public PaymentFailedEvent(String paymentId, String reason) {
        super();
        this.paymentId = paymentId;
        this.reason = reason;
    }
}

// Value Objects
package com.yourcompany.payment.domain.valueobject;

import lombok.Value;
import java.math.BigDecimal;
import java.util.Currency;

@Value
public class Money {
    BigDecimal amount;
    Currency currency;
    
    public Money(BigDecimal amount, String currencyCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = Currency.getInstance(currencyCode);
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency.getCurrencyCode());
    }
    
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency.getCurrencyCode());
    }
    
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }
}

@Value
public class InvoiceId {
    String value;
    
    public InvoiceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice ID cannot be null or empty");
        }
        this.value = value.trim();
    }
}

@Value
public class PaymentMethodId {
    String value;
    
    public PaymentMethodId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method ID cannot be null or empty");
        }
        this.value = value.trim();
    }
}

// Domain Exceptions
package com.yourcompany.payment.domain.exception;

public class PaymentDomainException extends RuntimeException {
    public PaymentDomainException(String message) {
        super(message);
    }
    
    public PaymentDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class IllegalPaymentStateException extends PaymentDomainException {
    public IllegalPaymentStateException(String message) {
        super(message);
    }
}

public class PaymentExpiredException extends PaymentDomainException {
    public PaymentExpiredException(String message) {
        super(message);
    }
}

public class InsufficientAuthorizationException extends PaymentDomainException {
    public InsufficientAuthorizationException(String message) {
        super(message);
    }
}

// Payment Status Enum
package com.yourcompany.payment.domain.model;

public enum PaymentStatus {
    PENDING("Payment is pending authorization"),
    AUTHORIZED("Payment is authorized, awaiting capture"),
    PARTIALLY_CAPTURED("Payment is partially captured"),
    CAPTURED("Payment is fully captured"),
    FAILED("Payment authorization or capture failed"),
    VOIDED("Payment authorization was voided"),
    REFUNDED("Payment was refunded after capture"),
    PARTIALLY_REFUNDED("Payment was partially refunded");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canBeAuthorized() {
        return this == PENDING;
    }
    
    public boolean canBeCaptured() {
        return this == AUTHORIZED || this == PARTIALLY_CAPTURED;
    }
    
    public boolean canBeVoided() {
        return this == AUTHORIZED || this == PARTIALLY_CAPTURED;
    }
    
    public boolean canBeRefunded() {
        return this == CAPTURED || this == PARTIALLY_REFUNDED;
    }
}

// Payment Aggregate Root
package com.yourcompany.payment.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import com.yourcompany.payment.domain.valueobject.*;
import com.yourcompany.payment.domain.event.*;
import com.yourcompany.payment.domain.exception.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    
    @Id
    private String id;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "invoice_id"))
    private InvoiceId invoiceId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "authorized_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money authorizedAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "captured_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "captured_currency"))
    })
    private Money capturedAmount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;
    
    // Domain Events (not persisted)
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
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
                String.format("Cannot authorize payment in status: %s", status)
            );
        }
        
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
        this.expiresAt = authorizedAt.plusDays(7); // Stripe default
        
        addDomainEvent(new PaymentAuthorizedEvent(
            this.id, 
            this.invoiceId.getValue(), 
            this.authorizedAmount.getAmount(), 
            this.authorizedAmount.getCurrencyCode()
        ));
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
            newTotalCaptured.getAmount()
        ));
    }
    
    public void voidAuthorization() {
        if (!status.canBeVoided()) {
            throw new IllegalPaymentStateException(
                String.format("Cannot void payment in status: %s", status)
            );
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
                String.format("Cannot capture payment in status: %s", status)
            );
        }
        
        if (isAuthorizationExpired()) {
            throw new PaymentExpiredException("Cannot capture expired authorization");
        }
        
        Money remainingAmount = getRemainingAuthorizationAmount();
        if (captureAmount.isGreaterThan(remainingAmount)) {
            throw new InsufficientAuthorizationException(
                String.format("Capture amount %s exceeds remaining authorization %s", 
                    captureAmount.getAmount(), remainingAmount.getAmount())
            );
        }
    }
}

// Domain Repository Interface
package com.yourcompany.payment.domain.repository;

import com.yourcompany.payment.domain.model.Payment;
import com.yourcompany.payment.domain.valueobject.InvoiceId;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(String id);
    Optional<Payment> findByInvoiceId(InvoiceId invoiceId);
    List<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findExpiringAuthorizations(LocalDateTime before);
    void delete(Payment payment);
}

// Domain Service
package com.yourcompany.payment.domain.service;

import com.yourcompany.payment.domain.model.Payment;
import com.yourcompany.payment.domain.valueobject.*;
import org.springframework.stereotype.Service;

@Service
public class PaymentDomainService {
    
    public Payment createPaymentForInvoice(String invoiceId, BigDecimal amount, String currency) {
        InvoiceId id = new InvoiceId(invoiceId);
        Money paymentAmount = new Money(amount, currency);
        
        return Payment.createPendingPayment(id, paymentAmount);
    }
    
    public boolean isPartialCaptureAllowed(Payment payment, Money captureAmount) {
        if (!payment.getStatus().canBeCaptured()) {
            return false;
        }
        
        Money remainingAmount = payment.getRemainingAuthorizationAmount();
        return !captureAmount.isGreaterThan(remainingAmount);
    }
    
    public Money calculateOptimalCaptureAmount(Payment payment, Money requestedAmount) {
        Money remainingAmount = payment.getRemainingAuthorizationAmount();
        
        if (requestedAmount.isGreaterThan(remainingAmount)) {
            return remainingAmount;
        }
        
        return requestedAmount;
    }
}