package com.queue.backend.tickets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.queue.backend.config.CacheConfig;

/**
 * Brise kesirani prikaz reda - ali tek NAKON commita.
 *
 * Zasto ne obican @CacheEvict na servisnoj metodi:
 *  1) kljuc je queueId, a metode za promjenu statusa primaju ticketId -
 *     red se zna tek kad se ticket ucita
 *  2) @CacheEvict bi obrisao unos dok transakcija jos traje. U tom prozoru
 *     drugi zahtjev procita STARO stanje iz baze (commit se jos nije desio),
 *     upise ga u kes, i ekran prikazuje stari broj do isteka TTL-a.
 *     Evict poslije commita zatvara taj prozor.
 *
 * Ako transakcija padne, sinhronizacija se ne izvrsi - a i ne treba,
 * nista se nije promijenilo.
 */
@Component
public class QueueTicketsCache {

    private static final Logger log = LoggerFactory.getLogger(QueueTicketsCache.class);

    private final CacheManager cacheManager;

    public QueueTicketsCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit(Long queueId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Nema transakcije (npr. poziv iz testa) - brisi odmah.
            evict(queueId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(queueId);
            }
        });
    }

    private void evict(Long queueId) {
        Cache cache = cacheManager.getCache(CacheConfig.QUEUE_TICKETS);
        if (cache == null) {
            return;
        }
        try {
            cache.evict(queueId);
        } catch (RuntimeException e) {
            // CacheErrorHandler iz CacheConfig-a stiti SAMO anotacije
            // (@Cacheable...). Direktan poziv na Cache ide mimo njega, pa
            // moramo sami. Bez ovoga: Redis padne -> afterCommit pukne ->
            // klijent dobije 500 za ticket koji je VEC upisan u bazu.
            // Cijena: unos ostaje u kesu do isteka TTL-a (30s).
            log.warn("Evict nije uspio za red {}, ekran moze kasniti do TTL: {}", queueId, e.getMessage());
        }
    }
}
