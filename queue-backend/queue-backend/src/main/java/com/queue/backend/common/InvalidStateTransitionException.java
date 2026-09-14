package com.queue.backend.common;

/**
 * Pokusaj prelaza koji state machine ne dozvoljava
 * (npr. zavrsiti ticket koji jos niko nije pozvao).
 * GlobalExceptionHandler ovo pretvara u 409.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String poruka) {
        super(poruka);
    }
}
