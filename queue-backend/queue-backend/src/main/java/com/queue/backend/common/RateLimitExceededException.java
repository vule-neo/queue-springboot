package com.queue.backend.common;

/** Korisnik je potrosio svoju kvotu. Nosi koliko sekundi treba sacekati. */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Previse zahtjeva, pokusaj ponovo za " + retryAfterSeconds + "s");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
