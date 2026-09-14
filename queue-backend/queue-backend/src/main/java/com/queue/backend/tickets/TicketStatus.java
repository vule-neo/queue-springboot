package com.queue.backend.tickets;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Vrijednosti moraju doslovno odgovarati ck_ticket_status u V1__init.sql.
 *
 * Ovdje zivi i tabela dozvoljenih prelaza. Bez biblioteke - obicna mapa.
 * State machine je samo pravilo "iz stanja X smijes samo u ova stanja";
 * sve preko toga je nepotrebna masinerija za sedam stanja.
 *
 *   WAITING -> CALLED -> SERVING -> COMPLETED
 *   WAITING -> CANCELLED
 *   CALLED  -> NO_SHOW
 *   CALLED  -> SKIPPED -> WAITING
 */
public enum TicketStatus {

    WAITING,
    CALLED,
    SERVING,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    SKIPPED;

    // Staticko polje se inicijalizuje NAKON konstanti, pa smijemo da ih
    // referenciramo. Prazan skup = zavrsno stanje, dalje se ne ide.
    private static final Map<TicketStatus, Set<TicketStatus>> DOZVOLJENI = Map.of(
            WAITING,   EnumSet.of(CALLED, CANCELLED),
            CALLED,    EnumSet.of(SERVING, NO_SHOW, SKIPPED),
            SERVING,   EnumSet.of(COMPLETED),
            SKIPPED,   EnumSet.of(WAITING),
            COMPLETED, EnumSet.noneOf(TicketStatus.class),
            CANCELLED, EnumSet.noneOf(TicketStatus.class),
            NO_SHOW,   EnumSet.noneOf(TicketStatus.class));

    /** Smije li se iz ovog stanja u zadato. */
    public boolean moze(TicketStatus cilj) {
        return DOZVOLJENI.get(this).contains(cilj);
    }

    /** Zavrsno stanje - iz njega nema izlaza. */
    public boolean jeZavrsno() {
        return DOZVOLJENI.get(this).isEmpty();
    }
}
