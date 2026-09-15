package com.queue.backend.tickets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.queue.backend.config.RabbitConfig;

/**
 * Jedina tacka iz koje dogadjaj o ticketu izlazi iz servisa.
 *
 * Dva koraka, namjerno razdvojena:
 *
 *  1) objavi()  - TicketService je zove USRED transakcije. Ne salje nista
 *     napolje, samo preda dogadjaj Springu (in-process event).
 *
 *  2) posalji() - Spring je pozove tek POSLIJE COMMITA. Tek tada ide
 *     WebSocket i Rabbit.
 *
 * Zasto ne slati odmah iz objavi(): ako commit padne (npr. pukne
 * constraint), ekran bi vec pokazao broj koji ne postoji, a Rabbit
 * potrosac bi upisao statistiku za ticket kojeg nema u bazi. Slanje
 * poslije commita to zatvara.
 *
 * Ostaje jedna rupa: commit prodje, a slanje na Rabbit pukne (Rabbit
 * ugasen) - dogadjaj je izgubljen. Rjesenje za to je outbox pattern
 * (event se upise u bazu u ISTOJ transakciji, poseban posao ga salje).
 * Svjesno ga ne radimo sad - za ovaj sistem je gubitak jednog SMS-a
 * prihvatljiv, a outbox donosi cijelu novu tabelu i scheduler.
 */
@Component
public class TicketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketEventPublisher.class);

    private final ApplicationEventPublisher springEvents;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;

    public TicketEventPublisher(ApplicationEventPublisher springEvents,
                                SimpMessagingTemplate messagingTemplate,
                                RabbitTemplate rabbitTemplate) {
        this.springEvents = springEvents;
        this.messagingTemplate = messagingTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Zove servis, unutar transakcije. Samo registruje dogadjaj. */
    public void objavi(TicketEvent dogadjaj) {
        springEvents.publishEvent(dogadjaj);
    }

    /**
     * Zove Spring, poslije commita. fallbackExecution: ako neko pozove
     * objavi() van transakcije (npr. iz testa), posalji odmah umjesto
     * da dogadjaj tiho nestane.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void posalji(TicketEvent dogadjaj) {
        // Jedan topic po redu - ekran u cekaonici slusa samo svoj red.
        messagingTemplate.convertAndSend("/topic/queue/" + dogadjaj.queueId(), dogadjaj);

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, dogadjaj.routingKey(), dogadjaj);
        } catch (RuntimeException e) {
            // Transakcija je vec commitovana - ticket postoji. Ne mozemo
            // "vratiti" HTTP odgovor u gresku zbog notifikacije. Logiraj
            // i nastavi; vidi napomenu o outbox patternu iznad.
            log.error("Rabbit nedostupan, dogadjaj {} [{}] izgubljen: {}",
                    dogadjaj.eventId(), dogadjaj.routingKey(), e.getMessage());
        }
    }
}
