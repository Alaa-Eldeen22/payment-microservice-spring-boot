// package com.paymenthub.payment_service.domain.service;

// import com.paymenthub.payment_service.domain.entity.Payment;
// import com.paymenthub.payment_service.domain.valueobject.InvoiceId;
// import com.paymenthub.payment_service.domain.valueobject.Money;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// import java.math.BigDecimal;
// import java.text.ParseException;

// /**
//  * Domain Service for payment authorization workflow
//  * Encapsulates the complex business process of creating and authorizing payments
//  * 
//  * Use when: Business logic spans multiple aggregates or requires external services
//  */
// @Service
// @Slf4j
// public class PaymentAuthorizationDomainService {
    
//     /**
//      * Creates a payment and attempts authorization
//      * 
//      * @param invoiceId Invoice to pay for
//      * @param amount Amount to authorize
//      * @param authorizeFunction Function that calls payment gateway
//      * @return The payment (either AUTHORIZED or FAILED)
//      */
//     public Payment createAndAuthorize(
//             InvoiceId invoiceId,
//             Money amount,
//             PaymentAuthorizationFunction authorizeFunction) {
        
//         log.info("Creating payment for invoice: {}", invoiceId.getValue());
        
//         // 1. Create payment in PENDING state
//         Payment payment = Payment.createPendingPayment(invoiceId, amount);
        
//         // 2. Attempt authorization
//         try {
//             String gatewayReferenceId = authorizeFunction.authorize();
            
//             // Success - transition to AUTHORIZED
//             payment.authorize(gatewayReferenceId);
//             log.info("Payment authorized successfully: {}", payment.getId());
            
//         } catch (ParseException e) {
//             // Failure - transition to FAILED
//             log.error("Payment authorization failed: {} - Reason: {}", 
//                 payment.getId(), e.getMessage());
//             payment.markAsFailed(e.getMessage());
//         }
        
//         return payment;
//     }
    
//     /**
//      * Functional interface for authorization logic
//      * Allows different implementations (automatic vs manual)
//      */
//     @FunctionalInterface
//     public interface PaymentAuthorizationFunction {
//         String authorize() throws ParseException;
//     }
// }