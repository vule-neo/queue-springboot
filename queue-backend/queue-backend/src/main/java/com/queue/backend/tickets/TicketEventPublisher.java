package com.queue.backend.tickets;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Salje dogadjaje na /topic/queue/{queueId}.
 *
 * Zasebna klasa a ne poziv iz TicketService-a direktno: kad u V5 dodje
 * RabbitMQ, mijenja se samo ova klasa - TicketService ostaje netaknut.
 */
@Component
public class TicketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public TicketEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void objavi(TicketEvent dogadjaj) {
        // Jedan topic po redu - ekran u cekaonici slusa samo svoj red
        // i ne dobija poruke o tudjim redovima.
        messagingTemplate.convertAndSend("/topic/queue/" + dogadjaj.queueId(), dogadjaj);
    }
}
