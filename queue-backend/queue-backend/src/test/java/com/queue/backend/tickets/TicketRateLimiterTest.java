package com.queue.backend.tickets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.queue.backend.common.RateLimitExceededException;

/**
 * Ide na pravi Redis (docker: redis:7-alpine na 6379), ne na mock -
 * ono sto testiramo je upravo da stanje kante zivi u Redisu.
 *
 * Svaki test koristi svoj nasumicni kljuc, pa se testovi ne mijesaju
 * ni medjusobno ni sa ostacima od proslog pokretanja (kljuc bi ionako
 * istekao za minutu, ali ne zelimo zavisiti od toga).
 */
@SpringBootTest
@DisplayName("Rate limit: 5 brojeva u minuti po korisniku")
class TicketRateLimiterTest {

    private static final int LIMIT = 5;

    @Autowired
    private TicketRateLimiter rateLimiter;

    @Test
    @DisplayName("prvih 5 prolazi, sesti dobija RateLimitExceededException")
    void sestiZahtjevPada() {
        String korisnik = "test-" + UUID.randomUUID() + "@test.com";

        for (int i = 1; i <= LIMIT; i++) {
            int redni = i;
            assertThatCode(() -> rateLimiter.provjeri(korisnik))
                    .as("zahtjev br. %d mora proci", redni)
                    .doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimiter.provjeri(korisnik))
                .isInstanceOf(RateLimitExceededException.class)
                // Kanta se puni 1 zeton / 12s, pa cekanje mora biti izmedju 1 i 12s.
                .satisfies(e -> {
                    long retry = ((RateLimitExceededException) e).getRetryAfterSeconds();
                    org.assertj.core.api.Assertions.assertThat(retry).isBetween(1L, 12L);
                });
    }

    @Test
    @DisplayName("kvota je po korisniku - drugi korisnik nije pogodjen")
    void kvotaJePoKorisniku() {
        String prvi = "a-" + UUID.randomUUID() + "@test.com";
        String drugi = "b-" + UUID.randomUUID() + "@test.com";

        for (int i = 0; i < LIMIT; i++) {
            rateLimiter.provjeri(prvi);
        }
        assertThatThrownBy(() -> rateLimiter.provjeri(prvi))
                .isInstanceOf(RateLimitExceededException.class);

        // Prvi je potrosio svoje, drugi tek pocinje.
        assertThatCode(() -> rateLimiter.provjeri(drugi)).doesNotThrowAnyException();
    }
}
