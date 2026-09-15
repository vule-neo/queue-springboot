package com.queue.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Topologija u RabbitMQ-u. Spring je sam kreira pri startu (RabbitAdmin
 * deklarise svaki Queue/Exchange/Binding bean) - vidi se u UI-ju na 15672.
 *
 *   TicketService ──► exchange "ticket.events" (topic)
 *                        │  routing key: ticket.created, ticket.called, ...
 *                        ├─ ticket.#  ──► queue "ticket.notifications" ──► NotificationListener
 *                        └─ ticket.#  ──► queue "ticket.analytics"     ──► AnalyticsListener
 *
 * TOPIC exchange a ne DIRECT: svaki potrosac bira sta ga zanima preko
 * sablona. Danas oba slusaju sve (ticket.#), sutra SMS servis moze slusati
 * samo ticket.called - bez promjene u producer-u.
 *
 * Svaki queue ima svoj dead-letter queue (DLQ): poruka koja pukne 3 puta
 * zavrsi tamo, gdje je covjek moze pogledati, umjesto da se vrti zauvijek
 * i blokira sve iza sebe.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ticket.events";
    public static final String NOTIFICATIONS_QUEUE = "ticket.notifications";
    public static final String ANALYTICS_QUEUE = "ticket.analytics";

    private static final String DLX = "ticket.events.dlx";
    private static final String DLQ_SUFFIX = ".dlq";

    // ---------- glavni tok ----------

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue notificationsQueue() {
        return saDlq(NOTIFICATIONS_QUEUE);
    }

    @Bean
    public Queue analyticsQueue() {
        return saDlq(ANALYTICS_QUEUE);
    }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue()).to(ticketExchange()).with("ticket.#");
    }

    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder.bind(analyticsQueue()).to(ticketExchange()).with("ticket.#");
    }

    // ---------- dead letter ----------

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Queue analyticsDlq() {
        return QueueBuilder.durable(ANALYTICS_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding notificationsDlqBinding() {
        return BindingBuilder.bind(notificationsDlq()).to(deadLetterExchange()).with(NOTIFICATIONS_QUEUE);
    }

    @Bean
    public Binding analyticsDlqBinding() {
        return BindingBuilder.bind(analyticsDlq()).to(deadLetterExchange()).with(ANALYTICS_QUEUE);
    }

    /**
     * Durable: prezivi restart Rabbita. Odbijena poruka (nack bez requeue)
     * ide na DLX sa routing key-em = ime queue-a, pa zavrsi u "<ime>.dlq".
     */
    private Queue saDlq(String ime) {
        return QueueBuilder.durable(ime)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(ime)
                .build();
    }

    // ---------- format poruke ----------

    /**
     * JSON umjesto Java serijalizacije (default). Tri razloga: poruka se
     * moze procitati u UI-ju, potrosac ne mora biti Java, i ne zavisi od
     * serialVersionUID-a. Springov ObjectMapper da datumi idu kao ISO
     * string, isto kao u REST odgovorima.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper springMapper) {
        return new Jackson2JsonMessageConverter(springMapper);
    }
}
