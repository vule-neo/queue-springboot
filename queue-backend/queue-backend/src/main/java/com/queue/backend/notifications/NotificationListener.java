package com.queue.backend.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.idempotency.IdempotencyGuard;
import com.queue.backend.config.RabbitConfig;
import com.queue.backend.tickets.TicketEvent;
import com.queue.backend.tickets.TicketStatus;

/**
 * Prvi potrosac: obavijesti musteriju.
 *
 * Za sada samo log - pravi SMS/email trazi provajdera i nije poenta V5.
 * Poenta je da TicketService NE ZNA da ovo postoji: da sutra dodas
 * push notifikacije, dodas jos jedan listener, servis se ne dira.
 *
 * Idempotency i ovdje iako "samo logujemo": duplikat SMS-a je pravi
 * problem (kosta, nervira), pa se navika gradi odmah.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private static final String CONSUMER = "notifications";

    private final IdempotencyGuard guard;

    public NotificationListener(IdempotencyGuard guard) {
        this.guard = guard;
    }

    /**
     * @Transactional: marker iz guard-a i "obrada" u istoj transakciji.
     * Exception ovdje = Rabbit redostavlja (3x, pa DLQ) - vidi properties.
     */
    @RabbitListener(queues = RabbitConfig.NOTIFICATIONS_QUEUE)
    @Transactional
    public void onTicketEvent(TicketEvent event) {
        if (!guard.prviPut(CONSUMER, event.eventId())) {
            log.info("[notifications] duplikat {} preskocen", event.eventId());
            return;
        }

        var t = event.ticket();
        switch (event.tip()) {
            case IZDAT -> log.info("[notifications] {} uzet u redu {} - ovdje bi isao SMS: 'Vas broj je {}'",
                    t.number(), event.queueId(), t.number());
            case PROMIJENJEN -> {
                if (t.status() == TicketStatus.CALLED) {
                    log.info("[notifications] {} POZVAN - ovdje bi isao SMS: 'Na redu ste, pridjite salteru'",
                            t.number());
                } else {
                    log.debug("[notifications] {} -> {} (nema obavijesti)", t.number(), t.status());
                }
            }
        }
    }
}
