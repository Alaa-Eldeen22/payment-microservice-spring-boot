package com.paymenthub.payment_service.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${INVOICE_EXCHANGE:invoice_events}")
    private String exchangeName;

    @Value("${INVOICE_QUEUE:invoice_events}")
    private String queueName;

    @Value("${INVOICE_ROUTING_KEY:invoice.created}")
    private String routingKey;

    @Bean
    public TopicExchange invoiceExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue invoiceQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding binding(Queue invoiceQueue, TopicExchange invoiceExchange) {
        return BindingBuilder
                .bind(invoiceQueue)
                .to(invoiceExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}