package com.paymenthub.payment_service.infrastructure.adapter.in.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymenthub.payment_service.application.port.in.command.AuthorizePaymentCommand;
import com.paymenthub.payment_service.application.port.in.usecase.AuthorizePaymentUseCase;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhooks/stripe")
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookController {

    private final AuthorizePaymentUseCase authorizePaymentUseCase;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    ResponseEntity<String> handleWebHook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook");
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (Exception e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "payment_intent.canceled" -> handlePaymentIntentCanceled(event);
            default -> log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook processed");
    }

    private void handlePaymentIntentSucceeded(Event event) {
        try {
            // Parse Stripe PaymentIntent from event
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalArgumentException("No PaymentIntent in event"));

            // Extract our payment ID from metadata
            String paymentId = paymentIntent.getMetadata().get("payment_id");
            if (paymentId == null) {
                log.error("No payment_id in PaymentIntent metadata");
                return;
            }

            log.info("Payment Intent succeeded: {} for payment: {}",
                    paymentIntent.getId(), paymentId);

            // Create command
            AuthorizePaymentCommand command = new AuthorizePaymentCommand(
                    paymentId,
                    paymentIntent.getId() // Stripe Payment Intent ID
            );

            // ← HERE: Call the authorize use case
            authorizePaymentUseCase.authorize(command);

            log.info("Payment authorized via webhook: {}", paymentId);

        } catch (Exception e) {
            log.error("Failed to handle payment_intent.succeeded", e);
            // Consider: send to dead letter queue for retry
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        // Handle failed payments
        log.info("Payment failed event received");
        // TODO: Call markAsFailedUseCase
    }

    private void handlePaymentIntentCanceled(Event event) {
        // Handle canceled payments
        log.info("Payment canceled event received");
        // TODO: Call voidPaymentUseCase
    }
}
