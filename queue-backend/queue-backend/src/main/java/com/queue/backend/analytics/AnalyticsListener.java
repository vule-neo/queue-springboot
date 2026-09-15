package com.queue.backend.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.idempotency.IdempotencyGuard;
import com.queue.backend.config.RabbitConfig;
import com.queue.backend.tickets.TicketEvent;

/**
 * Drugi potrosac: svaki dogadjaj upisi u ticket_event_log.
 *
 * Isti event kao notifications, ali SVOJ queue: Rabbit svakom queue-u da
 * kopiju, pa notifications i analytics rade nezavisno - ako jedan pukne,
 * drugi ne gubi poruku.
 */
@Component
public class AnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsListener.class);
    private static final String CONSUMER = "analytics";

    private final IdempotencyGuard guard;
    private final TicketEventLogRepository repository;

    public AnalyticsListener(IdempotencyGuard guard, TicketEventLogRepository repository) {
        this.guard = guard;
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitConfig.ANALYTICS_QUEUE)
    @Transactional
    public void onTicketEvent(TicketEvent event) {
        if (!guard.prviPut(CONSUMER, event.eventId())) {
            log.info("[analytics] duplikat {} preskocen", event.eventId());
            return;
        }

        repository.save(TicketEventLog.from(event));
        log.debug("[analytics] {} {} -> {}", event.ticket().number(), event.tip(), event.ticket().status());
    }
}
