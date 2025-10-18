// Input/Output DTOs (separate from commands)
package com.yourcompany.payment.application.dto;

import lombok.Value;
import lombok.Builder;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Input DTOs
@Value
@Builder
public class CreatePaymentRequest {
    @NotBlank(message = "Invoice ID is required")
    String invoiceId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency;
}

@Value
@Builder
public class AuthorizePaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;
    
    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId;
    
    String customerId; // Optional
}

@Value
@Builder
public class CapturePaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;
    
    @NotNull(message = "Capture amount is required")
    @DecimalMin(value = "0.01", message = "Capture amount must be greater than 0")
    BigDecimal amount;
    
    String description; // Optional
}

@Value
@Builder
public class VoidPaymentRequest {
    @NotBlank(message = "Payment ID is required")
    String paymentId;
    
    String reason; // Optional
}

@Value
@Builder
public class ProcessInvoiceRequest {
    @NotBlank(message = "Invoice ID is required")
    String invoiceId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    String currency;
    
    String customerId;
    String paymentMethodId;
}

// Output DTOs
@Value
@Builder
public class PaymentResponse {
    String id;
    String invoiceId;
    BigDecimal authorizedAmount;
    BigDecimal capturedAmount;
    String currency;
    String status;
    String stripePaymentIntentId;
    LocalDateTime createdAt;
    LocalDateTime authorizedAt;
    LocalDateTime capturedAt;
    LocalDateTime expiresAt;
    
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .invoiceId(payment.getInvoiceId().getValue())
            .authorizedAmount(payment.getAuthorizedAmount().getAmount())
            .capturedAmount(payment.getCapturedAmount().getAmount())
            .currency(payment.getAuthorizedAmount().getCurrencyCode())
            .status(payment.getStatus().name())
            .stripePaymentIntentId(payment.getStripePaymentIntentId())
            .createdAt(payment.getCreatedAt())
            .authorizedAt(payment.getAuthorizedAt())
            .capturedAt(payment.getCapturedAt())
            .expiresAt(payment.getExpiresAt())
            .build();
    }
}

@Value
@Builder
public class AuthorizationResponse {
    boolean success;
    String paymentId;
    BigDecimal authorizedAmount;
    String currency;
    LocalDateTime authorizedAt;
    LocalDateTime expiresAt;
    String errorMessage;
}

@Value
@Builder
public class CaptureResponse {
    boolean success;
    String paymentId;
    BigDecimal capturedAmount;
    BigDecimal totalCapturedAmount;
    String currency;
    LocalDateTime capturedAt;
    String errorMessage;
}

// Ports (Interfaces for external dependencies)
package com.yourcompany.payment.application.port.out;

import com.yourcompany.payment.domain.model.Payment;
import com.yourcompany.payment.domain.valueobject.Money;
import java.math.BigDecimal;

// Payment Gateway Port
public interface PaymentGatewayPort {
    AuthorizationResult authorizePayment(Payment payment, String paymentMethodId, String customerId);
    CaptureResult capturePayment(String gatewayTransactionId, Money amount);
    VoidResult voidAuthorization(String gatewayTransactionId);
}

// Result Objects
@lombok.Value
@lombok.Builder
public class AuthorizationResult {
    boolean success;
    String gatewayTransactionId;
    String errorMessage;
    String errorCode;
    
    public static AuthorizationResult success(String gatewayTransactionId) {
        return AuthorizationResult.builder()
            .success(true)
            .gatewayTransactionId(gatewayTransactionId)
            .build();
    }
    
    public static AuthorizationResult failure(String errorMessage, String errorCode) {
        return AuthorizationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .build();
    }
}

@lombok.Value
@lombok.Builder
public class CaptureResult {
    boolean success;
    BigDecimal capturedAmount;
    String errorMessage;
    String errorCode;
    
    public static CaptureResult success(BigDecimal capturedAmount) {
        return CaptureResult.builder()
            .success(true)
            .capturedAmount(capturedAmount)
            .build();
    }
    
    public static CaptureResult failure(String errorMessage, String errorCode) {
        return CaptureResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .build();
    }
}

@lombok.Value
@lombok.Builder
public class VoidResult {
    boolean success;
    String errorMessage;
    String errorCode;
    
    public static VoidResult success() {
        return VoidResult.builder().success(true).build();
    }
    
    public static VoidResult failure(String errorMessage, String errorCode) {
        return VoidResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .errorCode(errorCode)
            .build();
    }
}

// Event Publisher Port
public interface EventPublisherPort {
    void publishPaymentAuthorized(PaymentAuthorizedEvent event);
    void publishPaymentCaptured(PaymentCapturedEvent event);
    void publishPaymentFailed(PaymentFailedEvent event);
    void publishPaymentVoided(PaymentVoidedEvent event);
}

// Use Cases (Main Application Services)
package com.yourcompany.payment.application.usecase;

import com.yourcompany.payment.application.dto.*;
import com.yourcompany.payment.application.port.out.*;
import com.yourcompany.payment.domain.model.Payment;
import com.yourcompany.payment.domain.repository.PaymentRepository;
import com.yourcompany.payment.domain.service.PaymentDomainService;
import com.yourcompany.payment.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final PaymentDomainService paymentDomainService;
    
    @Transactional
    public PaymentResponse execute(CreatePaymentRequest request) {
        log.info("Creating payment for invoice: {}", request.getInvoiceId());
        
        // Check if payment already exists for this invoice
        Optional<Payment> existingPayment = paymentRepository.findByInvoiceId(
            new InvoiceId(request.getInvoiceId())
        );
        
        if (existingPayment.isPresent()) {
            log.warn("Payment already exists for invoice: {}", request.getInvoiceId());
            return PaymentResponse.from(existingPayment.get());
        }
        
        // Create new payment using domain service
        Payment payment = paymentDomainService.createPaymentForInvoice(
            request.getInvoiceId(),
            request.getAmount(),
            request.getCurrency()
        );
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created successfully: {}", savedPayment.getId());
        
        return PaymentResponse.from(savedPayment);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizePaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public AuthorizationResponse execute(AuthorizePaymentRequest request) {
        log.info("Authorizing payment: {}", request.getPaymentId());
        
        Payment payment = paymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException(request.getPaymentId()));
        
        try {
            // Call payment gateway
            AuthorizationResult result = paymentGateway.authorizePayment(
                payment, 
                request.getPaymentMethodId(), 
                request.getCustomerId()
            );
            
            if (result.isSuccess()) {
                // Update domain model
                payment.authorize(result.getGatewayTransactionId());
                
                // Save and publish events
                Payment savedPayment = paymentRepository.save(payment);
                publishDomainEvents(savedPayment);
                
                log.info("Payment authorized successfully: {}", payment.getId());
                
                return AuthorizationResponse.builder()
                    .success(true)
                    .paymentId(savedPayment.getId())
                    .authorizedAmount(savedPayment.getAuthorizedAmount().getAmount())
                    .currency(savedPayment.getAuthorizedAmount().getCurrencyCode())
                    .authorizedAt(savedPayment.getAuthorizedAt())
                    .expiresAt(savedPayment.getExpiresAt())
                    .build();
            } else {
                // Handle authorization failure
                payment.markAsFailed(result.getErrorMessage());
                Payment savedPayment = paymentRepository.save(payment);
                publishDomainEvents(savedPayment);
                
                log.error("Payment authorization failed: {} - {}", payment.getId(), result.getErrorMessage());
                
                return AuthorizationResponse.builder()
                    .success(false)
                    .paymentId(payment.getId())
                    .errorMessage(result.getErrorMessage())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during payment authorization: {}", payment.getId(), e);
            payment.markAsFailed(e.getMessage());
            paymentRepository.save(payment);
            
            return AuthorizationResponse.builder()
                .success(false)
                .paymentId(payment.getId())
                .errorMessage("Payment processing failed: " + e.getMessage())
                .build();
        }
    }
    
    private void publishDomainEvents(Payment payment) {
        payment.getDomainEvents().forEach(event -> {
            if (event instanceof PaymentAuthorizedEvent) {
                eventPublisher.publishPaymentAuthorized((PaymentAuthorizedEvent) event);
            } else if (event instanceof PaymentFailedEvent) {
                eventPublisher.publishPaymentFailed((PaymentFailedEvent) event);
            }
        });
        payment.clearDomainEvents();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class CapturePaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final EventPublisherPort eventPublisher;
    private final PaymentDomainService paymentDomainService;
    
    @Transactional
    public CaptureResponse execute(CapturePaymentRequest request) {
        log.info("Capturing payment: {} with amount: {}", request.getPaymentId(), request.getAmount());
        
        Payment payment = paymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException(request.getPaymentId()));
        
        try {
            Money captureAmount = new Money(request.getAmount(), payment.getAuthorizedAmount().getCurrencyCode());
            
            // Validate capture operation through domain service
            if (!paymentDomainService.isPartialCaptureAllowed(payment, captureAmount)) {
                log.error("Capture not allowed for payment: {}", payment.getId());
                return CaptureResponse.builder()
                    .success(false)
                    .paymentId(payment.getId())
                    .errorMessage("Capture amount exceeds available authorization")
                    .build();
            }
            
            // Call payment gateway
            CaptureResult result = paymentGateway.capturePayment(
                payment.getStripePaymentIntentId(), 
                captureAmount
            );
            
            if (result.isSuccess()) {
                // Update domain model
                payment.capture(captureAmount);
                
                // Save and publish events
                Payment savedPayment = paymentRepository.save(payment);
                publishDomainEvents(savedPayment);
                
                log.info("Payment captured successfully: {}", payment.getId());
                
                return CaptureResponse.builder()
                    .success(true)
                    .paymentId(savedPayment.getId())
                    .capturedAmount(captureAmount.getAmount())
                    .totalCapturedAmount(savedPayment.getCapturedAmount().getAmount())
                    .currency(savedPayment.getAuthorizedAmount().getCurrencyCode())
                    .capturedAt(savedPayment.getCapturedAt())
                    .build();
            } else {
                log.error("Payment capture failed: {} - {}", payment.getId(), result.getErrorMessage());
                
                return CaptureResponse.builder()
                    .success(false)
                    .paymentId(payment.getId())
                    .errorMessage(result.getErrorMessage())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during payment capture: {}", payment.getId(), e);
            
            return CaptureResponse.builder()
                .success(false)
                .paymentId(payment.getId())
                .errorMessage("Payment capture failed: " + e.getMessage())
                .build();
        }
    }
    
    private void publishDomainEvents(Payment payment) {
        payment.getDomainEvents().forEach(event -> {
            if (event instanceof PaymentCapturedEvent) {
                eventPublisher.publishPaymentCaptured((PaymentCapturedEvent) event);
            }
        });
        payment.clearDomainEvents();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class VoidPaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGateway;
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public PaymentResponse execute(VoidPaymentRequest request) {
        log.info("Voiding payment: {}", request.getPaymentId());
        
        Payment payment = paymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException(request.getPaymentId()));
        
        try {
            // Call payment gateway
            VoidResult result = paymentGateway.voidAuthorization(payment.getStripePaymentIntentId());
            
            if (result.isSuccess()) {
                // Update domain model
                payment.voidAuthorization();
                
                // Save and publish events
                Payment savedPayment = paymentRepository.save(payment);
                publishDomainEvents(savedPayment);
                
                log.info("Payment voided successfully: {}", payment.getId());
                return PaymentResponse.from(savedPayment);
                
            } else {
                log.error("Payment void failed: {} - {}", payment.getId(), result.getErrorMessage());
                throw new PaymentProcessingException("Failed to void payment: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during payment void: {}", payment.getId(), e);
            throw new PaymentProcessingException("Payment void failed", e);
        }
    }
    
    private void publishDomainEvents(Payment payment) {
        payment.getDomainEvents().forEach(event -> {
            if (event instanceof PaymentVoidedEvent) {
                eventPublisher.publishPaymentVoided((PaymentVoidedEvent) event);
            }
        });
        payment.clearDomainEvents();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessInvoiceCreatedUseCase {
    
    private final CreatePaymentUseCase createPaymentUseCase;
    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    
    @Transactional
    public PaymentResponse execute(ProcessInvoiceRequest request) {
        log.info("Processing invoice created event for invoice: {}", request.getInvoiceId());
        
        // Step 1: Create payment
        CreatePaymentRequest createRequest = CreatePaymentRequest.builder()
            .invoiceId(request.getInvoiceId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .build();
            
        PaymentResponse payment = createPaymentUseCase.execute(createRequest);
        
        // Step 2: Authorize payment if payment method is provided
        if (request.getPaymentMethodId() != null) {
            AuthorizePaymentRequest authorizeRequest = AuthorizePaymentRequest.builder()
                .paymentId(payment.getId())
                .paymentMethodId(request.getPaymentMethodId())
                .customerId(request.getCustomerId())
                .build();
                
            AuthorizationResponse authResult = authorizePaymentUseCase.execute(authorizeRequest);
            
            if (!authResult.isSuccess()) {
                log.error("Failed to authorize payment for invoice: {} - {}", 
                    request.getInvoiceId(), authResult.getErrorMessage());
                // Payment will remain in failed state, which is correct
            }
        }
        
        return payment;
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    
    public PaymentResponse execute(String paymentId) {
        log.debug("Getting payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            
        return PaymentResponse.from(payment);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPaymentsByInvoiceUseCase {
    
    private final PaymentRepository paymentRepository;
    
    public List<PaymentResponse> execute(String invoiceId) {
        log.debug("Getting payments for invoice: {}", invoiceId);
        
        Optional<Payment> payment = paymentRepository.findByInvoiceId(new InvoiceId(invoiceId));
        
        return payment.map(p -> List.of(PaymentResponse.from(p)))
                     .orElse(List.of());
    }
}

// Application Exceptions
package com.yourcompany.payment.application.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String paymentId) {
        super("Payment not found with ID: " + paymentId);
    }
}

public class PaymentAuthorizationFailedException extends RuntimeException {
    private final String errorCode;
    
    public PaymentAuthorizationFailedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}