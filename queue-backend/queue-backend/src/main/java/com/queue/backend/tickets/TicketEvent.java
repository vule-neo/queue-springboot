package com.queue.backend.tickets;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dogadjaj nad ticketom. Isti objekat ide na dva kanala:
 *  - WebSocket (/topic/queue/{queueId}) - browseru, ekranu u cekaonici
 *  - RabbitMQ (exchange ticket.events)  - drugim dijelovima backenda
 *
 * Namjerno nosi CIJELI ticket, ne samo id: primalac treba odmah prikazati
 * broj ili upisati statistiku, bez dodatnog upita u bazu.
 *
 * eventId: Rabbit garantuje "bar jednom", ne "tacno jednom" - isti event
 * moze stici dvaput. Po ovom id-u potrosac prepozna da ga je vec obradio.
 */
public record TicketEvent(
        String eventId,
        Tip tip,
        Long queueId,
        TicketResponse ticket,
        OffsetDateTime kada) {

    public enum Tip {
        IZDAT,       // novi broj uzet
        PROMIJENJEN  // status se promijenio (pozvan, zavrsen, otkazan...)
    }

    public static TicketEvent izdat(Ticket t) {
        return new TicketEvent(UUID.randomUUID().toString(), Tip.IZDAT, t.getQueue().getId(),
                TicketResponse.from(t), OffsetDateTime.now());
    }

    public static TicketEvent promijenjen(Ticket t) {
        return new TicketEvent(UUID.randomUUID().toString(), Tip.PROMIJENJEN, t.getQueue().getId(),
                TicketResponse.from(t), OffsetDateTime.now());
    }

    /**
     * Routing key za Rabbit: "ticket.created", "ticket.called", "ticket.completed"...
     * Potrosac se preko sablona (ticket.#, ticket.called) pretplati samo na
     * ono sto ga zanima. Nije record komponenta pa ne ide u JSON.
     */
    public String routingKey() {
        String sufiks = tip == Tip.IZDAT ? "created" : ticket.status().name().toLowerCase();
        return "ticket." + sufiks;
    }
}
