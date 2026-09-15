package com.queue.backend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.queue.backend.config.RabbitConfig;
import com.queue.backend.tickets.TicketEvent;
import com.queue.backend.tickets.TicketResponse;
import com.queue.backend.tickets.TicketStatus;

/**
 * Pravi Rabbit (docker), pravi listener, prava baza.
 *
 * Simulira ono sto Rabbit radi kod "at-least-once": ISTA poruka stigne
 * dvaput. Bez IdempotencyGuard-a bi u ticket_event_log bila dva reda
 * (ili bi drugi pukao na UNIQUE i zavrsio u DLQ - takodje pogresno,
 * jer to nije greska nego ocekivan duplikat).
 *
 * Asinhrono: listener radi u drugoj niti, pa ne mozemo odmah provjeriti
 * bazu - await() ceka dok se prvi red ne pojavi, pa jos malo da bi se
 * eventualni drugi stigao upisati.
 */
@SpringBootTest
@DisplayName("Analytics consumer: isti event dvaput = jedan red")
class AnalyticsIdempotencyTest {

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired TicketEventLogRepository repository;

    @Test
    void duplikatSeUpisujeJednom() {
        // Pravi UUID (36 znakova) - event_id je VARCHAR(36). Prefiks "test-"
        // je prvi put probio duzinu, insert pao, poruka zavrsila u DLQ.
        String eventId = UUID.randomUUID().toString();
        TicketResponse ticket = new TicketResponse(999_999L, 1L, "T999", 999, LocalDate.now(),
                TicketStatus.WAITING, OffsetDateTime.now(), null, null);
        TicketEvent event = new TicketEvent(eventId, TicketEvent.Tip.IZDAT, 1L, ticket, OffsetDateTime.now());

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, event.routingKey(), event);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, event.routingKey(), event);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(repository.countByEventId(eventId)).isEqualTo(1));

        // Pauza pa ponovo: da drugi upis nije "samo jos u putu".
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(repository.countByEventId(eventId)).isEqualTo(1));

        TicketEventLog red = repository.findByEventId(eventId).orElseThrow();
        assertThat(red.getTicketNumber()).isEqualTo("T999");
        assertThat(red.getStatus()).isEqualTo(TicketStatus.WAITING);
        assertThat(red.getEventType()).isEqualTo(TicketEvent.Tip.IZDAT);
    }
}
