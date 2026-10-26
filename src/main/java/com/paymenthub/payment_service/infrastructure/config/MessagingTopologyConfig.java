package com.paymenthub.payment_service.infrastructure.config;

import static com.paymenthub.payment_service.infrastructure.messaging.MessagingConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * v1 topology from MESSAGING-CONTRACT.md §3: both topic exchanges, plus the
 * queue this service consumes with its retry ladder and DLX/DLQ. Declared by
 * the auto-configured RabbitAdmin on connect.
 */
@Configuration
public class MessagingTopologyConfig {

    /** Contract values are 5s/30s/2m. Only override against a throw-away broker. */
    @Value("${messaging.retry-delays-ms:5000,30000,120000}")
    private List<Long> retryDelaysMs;

    @Bean
    public Declarables exchanges() {
        return new Declarables(
                ExchangeBuilder.topicExchange(INVOICE_EVENTS_EXCHANGE).durable(true).build(),
                ExchangeBuilder.topicExchange(PAYMENT_EVENTS_EXCHANGE).durable(true).build());
    }

    @Bean
    public Declarables invoiceEventsConsumerTopology() {
        String queue = INVOICE_EVENTS_QUEUE;
        List<Declarable> d = new ArrayList<>();

        FanoutExchange dlx = ExchangeBuilder.fanoutExchange(deadLetterExchange(queue)).durable(true).build();
        Queue dlq = QueueBuilder.durable(deadLetterQueue(queue)).quorum().build();
        d.add(dlx);
        d.add(dlq);
        d.add(BindingBuilder.bind(dlq).to(dlx));

        DirectExchange retryEx = ExchangeBuilder.directExchange(retryExchange(queue)).durable(true).build();
        d.add(retryEx);
        for (int i = 0; i < retryDelaysMs.size(); i++) {
            int step = i + 1;
            Queue rq = QueueBuilder.durable(retryQueue(queue, step))
                    .ttl(retryDelaysMs.get(i).intValue())
                    // back to *our* queue via the default exchange, not via
                    // the topic exchange (would re-fan to other consumers)
                    .deadLetterExchange("")
                    .deadLetterRoutingKey(queue)
                    .build();
            d.add(rq);
            d.add(BindingBuilder.bind(rq).to(retryEx).with(String.valueOf(step)));
        }

        Queue main = QueueBuilder.durable(queue)
                .quorum()
                .deadLetterExchange(dlx.getName())
                .deliveryLimit(MAIN_QUEUE_DELIVERY_LIMIT)
                .build();
        d.add(main);
        TopicExchange source = new TopicExchange(INVOICE_EVENTS_EXCHANGE, true, false);
        for (String rk : List.of(INVOICE_CREATED, INVOICE_RETRIED)) {
            Binding b = BindingBuilder.bind(main).to(source).with(rk);
            d.add(b);
        }
        return new Declarables(d);
    }
}
