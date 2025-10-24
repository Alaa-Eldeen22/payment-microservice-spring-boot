package com.paymenthub.payment_service.infrastructure.adapter.in.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.model.Event;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhooks/stripe")
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookController {

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
        // Handle succeeded payments
        log.info("Payment succeeded event received");
        // TODO: Call martAsCapturedUseCase
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
