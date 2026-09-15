package com.queue.backend.analytics;

import com.queue.backend.tickets.TicketStatus;

/**
 * Procjena cekanja za jedan ticket.
 *
 * basis kaze odakle je prosjek: HISTORY (iz zavrsenih ticketa ovog reda)
 * ili DEFAULT (konstanta, red jos nema istoriju). Klijent to moze prikazati
 * kao "procjena" vs "gruba procjena".
 */
public record WaitEstimateResponse(
        Long ticketId,
        String number,
        TicketStatus status,
        long ahead,
        long avgServiceSeconds,
        long estimatedWaitSeconds,
        Basis basis) {

    public enum Basis { HISTORY, DEFAULT }
}
