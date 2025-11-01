package com.paymenthub.payment_service.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${INVOICE_EXCHANGE:invoice_events}")
    private String exchangeName;

    @Value("${INVOICE_QUEUE:invoice_events}")
    private String invoice_events_queue_name;

    @Value("${PAYMENT_QUEUE:payment_events}")
    private String payment_events_queue_name;

    @Value("${INVOICE_CREATED_ROUTING_KEY:invoice.created}")
    private String invoiceCreatedRoutingKey;

    @Value("${INVOICE_RETRIED_ROUTING_KEY:invoice.retried}")
    private String invoiceRetriedRoutingKey;

    @Value("${PAYMENT_AUTHORIZED_ROUTING_KEY:payment.authorized}")
    private String paymentAuthorizedRoutingKey;

    @Value("${PAYMENT_FAILED_ROUTING_KEY:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${PAYMENT_CAPTURED_ROUTING_KEY:payment.captured}")
    private String paymentCapturedRoutingKey;

    @Value("${PAYMENT_VOIDED_ROUTING_KEY:payment.voided}")
    private String paymentVoidedRoutingKey;

    @Bean
    public TopicExchange invoiceExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue invoiceQueue() {
        return new Queue(invoice_events_queue_name, true);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(payment_events_queue_name, true);
    }

    @Bean
    public Binding invoiceCreatedBinding(Queue invoiceQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(invoiceQueue)
                .to(invoiceExchange)
                .with(invoiceCreatedRoutingKey);
    }

    @Bean
    public Binding invoiceRetriedBinding(Queue invoiceQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(invoiceQueue)
                .to(invoiceExchange)
                .with(invoiceRetriedRoutingKey);
    }

    @Bean
    public Binding paymentAuthorizedBinding(Queue paymentQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(invoiceExchange).with(paymentAuthorizedRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(invoiceExchange)
                .with(paymentFailedRoutingKey);
    }

    @Bean
    public Binding paymentCapturedBinding(Queue paymentQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(invoiceExchange)
                .with(paymentCapturedRoutingKey);
    }

    @Bean
    public Binding paymentVoidedBinding(Queue paymentQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(paymentQueue)
                .to(invoiceExchange)
                .with(paymentVoidedRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}