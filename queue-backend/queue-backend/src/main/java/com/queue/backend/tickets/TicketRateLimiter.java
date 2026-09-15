package com.queue.backend.tickets;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.queue.backend.common.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * Ogranicava koliko brojeva jedan korisnik smije uzeti u minuti.
 *
 * Token bucket: svaki korisnik ima kantu sa N zetona. Svaki zahtjev
 * potrosi jedan; kanta se dopunjava brzinom N po minuti. Prazna kanta
 * = 429. Za razliku od "N zahtjeva po kalendarskoj minuti", ovo nema
 * rupu na granici minute (5 u 12:00:59 + 5 u 12:01:00 = 10 u sekundi).
 */
@Component
public class TicketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TicketRateLimiter.class);
    private static final String KEY_PREFIX = "ratelimit:tickets:";

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> configuration;

    public TicketRateLimiter(ProxyManager<String> proxyManager,
                             @Value("${app.ratelimit.tickets.per-minute}") long perMinute) {
        this.proxyManager = proxyManager;
        // refillGreedy: zetoni se vracaju ravnomjerno (1 na svakih 60/N sekundi),
        // ne svih N odjednom na pocetku minute. Glatkiji za korisnika.
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        this.configuration = () -> BucketConfiguration.builder().addLimit(limit).build();
    }

    /**
     * Potrosi jedan zeton za korisnika ili baci RateLimitExceededException.
     *
     * @param korisnik ono sto jedinstveno identifikuje korisnika (email iz
     *                 tokena). NE IP adresa: cijela cekaonica na istom
     *                 WiFi-ju dijeli jedan IP, pa bi jedan covjek blokirao sve.
     */
    public void provjeri(String korisnik) {
        ConsumptionProbe probe;
        try {
            probe = proxyManager.builder()
                    .build(KEY_PREFIX + korisnik, configuration)
                    .tryConsumeAndReturnRemaining(1);
        } catch (RuntimeException e) {
            // FAIL-OPEN, svjesna odluka: rate limit stiti od zloupotrebe,
            // nije sigurnosna granica. Ako Redis padne, bolje je da ljudi
            // mogu uzeti broj bez limita nego da niko ne moze uzeti broj.
            // Za login ili placanje bi odluka bila suprotna (fail-closed).
            log.warn("Rate limiter nedostupan, propustam [{}]: {}", korisnik, e.getMessage());
            return;
        }

        if (!probe.isConsumed()) {
            long cekajSekundi = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            throw new RateLimitExceededException(cekajSekundi);
        }
    }
}
