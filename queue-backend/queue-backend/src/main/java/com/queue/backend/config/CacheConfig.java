package com.queue.backend.config;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queue.backend.tickets.TicketResponse;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;

/**
 * Redis kes za javni ekran u cekaonici.
 *
 * Zasto kes bas ovdje: GET /api/queues/{id}/tickets je jedini endpoint bez
 * tokena, gadja ga svaki ekran na zidu pri svakom ucitavanju, a odgovor je
 * isti za sve. Ostali endpointi su po korisniku ili mijenjaju stanje - tu
 * kes nema sta da radi.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String QUEUE_TICKETS = "queueTickets";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${app.cache.queue-tickets-ttl-seconds}")
    private long queueTicketsTtlSeconds;

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer(ObjectMapper springMapper) {
        // Tipizirani JSON serializer umjesto podrazumijevanog JDK:
        //  - record TicketResponse nije Serializable, JDK bi pukao
        //  - JSON u Redisu se moze procitati golim okom (redis-cli), JDK
        //    binarni blob ne moze
        //  - koristimo Springov ObjectMapper pa OffsetDateTime/LocalDate
        //    idu istim formatom kao i u HTTP odgovoru
        // Tacan JavaType (List<TicketResponse>) umjesto "generic" serializera
        // sa @class poljima: nema polimorfizma, pa nema ni razloga da tip
        // klase zavrsi u Redisu.
        JavaType listType = springMapper.getTypeFactory()
                .constructCollectionType(List.class, TicketResponse.class);
        var serializer = new Jackson2JsonRedisSerializer<List<TicketResponse>>(springMapper, listType);

        RedisCacheConfiguration queueTickets = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(queueTicketsTtlSeconds))
                .serializeValuesWith(SerializationPair.fromSerializer(serializer))
                // Prazan red je legitiman odgovor ([]), nije "nema podataka".
                // Null se nikad ne vraca pa ga nema smisla ni kesirati.
                .disableCachingNullValues();

        return builder -> builder.withCacheConfiguration(QUEUE_TICKETS, queueTickets);
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer() {
        // Dok Lettuce ZNA da je veza pukla, podrazumijevano ipak stavlja
        // komande u red i ceka reconnect - do isteka timeouta. Nama treba
        // suprotno: odbij odmah, pa CacheErrorHandler ispod prebaci na bazu.
        // Auto-reconnect ostaje ukljucen, cim se Redis vrati kes proradi sam.
        return builder -> builder.clientOptions(ClientOptions.builder()
                .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
                .build());
    }

    /**
     * Redis nije izvor istine - baza jeste. Ako Redis padne, javni ekran
     * treba i dalje da radi (sporije), a ne da vraca 500. Zato se greske
     * kesa logiraju i gutaju: @Cacheable tada jednostavno pozove metodu.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Kes GET nije uspio [{}:{}], citam iz baze: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Kes PUT nije uspio [{}:{}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                // Najopasniji slucaj: neuspio evict = stale podatak do isteka
                // TTL-a. Zato TTL i postoji.
                log.warn("Kes EVICT nije uspio [{}:{}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Kes CLEAR nije uspio [{}]: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
