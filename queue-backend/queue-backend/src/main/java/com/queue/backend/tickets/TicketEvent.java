package com.queue.backend.tickets;

import java.time.OffsetDateTime;

/**
 * Ono sto stize na ekran kroz WebSocket.
 *
 * Namjerno nosi CIJELI ticket, ne samo id: primalac je javni ekran u
 * cekaonici koji treba odmah prikazati broj, bez dodatnog REST poziva.
 */
public record TicketEvent(
        Tip tip,
        Long queueId,
        TicketResponse ticket,
        OffsetDateTime kada) {

    public enum Tip {
        IZDAT,       // novi broj uzet
        PROMIJENJEN  // status se promijenio (pozvan, zavrsen, otkazan...)
    }

    public static TicketEvent izdat(Ticket t) {
        return new TicketEvent(Tip.IZDAT, t.getQueue().getId(),
                TicketResponse.from(t), OffsetDateTime.now());
    }

    public static TicketEvent promijenjen(Ticket t) {
        return new TicketEvent(Tip.PROMIJENJEN, t.getQueue().getId(),
                TicketResponse.from(t), OffsetDateTime.now());
    }
}
