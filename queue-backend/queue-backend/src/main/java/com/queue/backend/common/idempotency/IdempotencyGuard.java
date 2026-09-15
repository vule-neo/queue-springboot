package com.queue.backend.common.idempotency;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Jesam li ovaj event vec obradio?" - zajednicki mehanizam za sve
 * Rabbit potrosace.
 *
 * Nacin upotrebe (u listeneru, unutar @Transactional):
 *
 *   if (!guard.prviPut("analytics", event.eventId())) return;
 *   ... obrada ...
 *
 * Kljucno: marker i obrada su u ISTOJ transakciji. Ako obrada pukne,
 * rollback obrise i marker, Rabbit redostavi poruku, i drugi pokusaj
 * prodje kao prvi. Da je marker u svojoj transakciji (REQUIRES_NEW),
 * ostao bi upisan i poslije neuspjele obrade - event bi bio izgubljen.
 *
 * Zato MANDATORY: poziv van transakcije je greska u kodu, ne tihi
 * "radi nekako".
 */
@Component
public class IdempotencyGuard {

    private final ProcessedEventRepository repository;

    public IdempotencyGuard(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /**
     * @return true ako event jos nije obradjen (i sad je oznacen),
     *         false ako je duplikat - preskoci obradu.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean prviPut(String consumer, String eventId) {
        String id = consumer + ":" + eventId;

        if (repository.existsById(id)) {
            return false;
        }

        // Trka: dvije dostave istog eventa u istom trenutku obje prodju
        // existsById. Druga onda pukne na PRIMARY KEY pri commitu ->
        // rollback -> Rabbit je redostavi -> existsById sad vrati true.
        // Rijetko (Rabbit redostavlja tek posle nack-a), a ispravno.
        repository.save(new ProcessedEvent(id));
        return true;
    }
}
