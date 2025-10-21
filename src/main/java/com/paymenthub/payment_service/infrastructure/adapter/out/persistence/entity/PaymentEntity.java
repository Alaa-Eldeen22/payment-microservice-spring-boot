package com.paymenthub.payment_service.infrastructure.adapter.out.persistence.entity;

import com.paymenthub.payment_service.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class PaymentEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String invoiceId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal authorizedAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal capturedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String stripePaymentIntentId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime authorizedAt;

    private LocalDateTime capturedAt;

    private LocalDateTime expiresAt;
}