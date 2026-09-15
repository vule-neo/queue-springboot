package com.queue.backend.tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.queue.backend.config.RabbitConfig;

/**
 * Cisti unit test, bez Springa i bez Rabbita: mockovi umjesto pravih
 * template-a. Testira samo LOGIKU publishera - na koji exchange, s kojim
 * kljucem, i sta se desi kad Rabbit ne radi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEventPublisher")
class TicketEventPublisherTest {

    @Mock ApplicationEventPublisher springEvents;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks TicketEventPublisher publisher;

    @Test
    @DisplayName("objavi() ne salje nista napolje - samo predaje Springu")
    void objaviSamoRegistruje() {
        TicketEvent e = event(TicketEvent.Tip.IZDAT, TicketStatus.WAITING);

        publisher.objavi(e);

        verify(springEvents).publishEvent(e);
        // Nista na WebSocket ni Rabbit dok transakcija ne prodje.
        org.mockito.Mockito.verifyNoInteractions(messagingTemplate, rabbitTemplate);
    }

    @Test
    @DisplayName("posalji() ide na WebSocket topic reda i na Rabbit exchange s pravim kljucem")
    void posaljiIdeNaObaKanala() {
        TicketEvent e = event(TicketEvent.Tip.PROMIJENJEN, TicketStatus.CALLED);

        publisher.posalji(e);

        verify(messagingTemplate).convertAndSend("/topic/queue/1", e);
        verify(rabbitTemplate).convertAndSend(RabbitConfig.EXCHANGE, "ticket.called", e);
    }

    @Test
    @DisplayName("routing key: IZDAT -> ticket.created, PROMIJENJEN -> ticket.<status>")
    void routingKey() {
        assertThat(event(TicketEvent.Tip.IZDAT, TicketStatus.WAITING).routingKey())
                .isEqualTo("ticket.created");
        assertThat(event(TicketEvent.Tip.PROMIJENJEN, TicketStatus.COMPLETED).routingKey())
                .isEqualTo("ticket.completed");
    }

    @Test
    @DisplayName("Rabbit ugasen: greska se loguje, ne propagira - ticket je vec commitovan")
    void rabbitPadNeRusiPoziv() {
        TicketEvent e = event(TicketEvent.Tip.IZDAT, TicketStatus.WAITING);
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), any(String.class), eq(e));

        assertThatCode(() -> publisher.posalji(e)).doesNotThrowAnyException();

        // WebSocket je ipak otisao - kanali su nezavisni.
        verify(messagingTemplate).convertAndSend("/topic/queue/1", e);
    }

    private static TicketEvent event(TicketEvent.Tip tip, TicketStatus status) {
        TicketResponse t = new TicketResponse(42L, 1L, "A042", 42, LocalDate.now(), status,
                OffsetDateTime.now(), null, null);
        return new TicketEvent("evt-1", tip, 1L, t, OffsetDateTime.now());
    }
}
