package com.paymenthub.payment_service.domain.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.paymenthub.payment_service.domain.entity.Payment;
import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
import com.paymenthub.payment_service.domain.valueobject.Money;

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