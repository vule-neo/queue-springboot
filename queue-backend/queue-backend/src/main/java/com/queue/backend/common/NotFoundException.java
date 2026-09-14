package com.queue.backend.common;

/**
 * Trazeni red u bazi ne postoji. GlobalExceptionHandler ovo pretvara u 404.
 * RuntimeException (a ne checked) da servisi ne moraju nositi throws deklaracije.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String poruka) {
        super(poruka);
    }
}
