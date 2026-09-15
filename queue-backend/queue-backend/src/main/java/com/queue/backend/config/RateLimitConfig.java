package com.queue.backend.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * Bucket4j nad Redisom.
 *
 * Zasto Redis a ne obicna mapa u memoriji: brojac mora biti ZAJEDNICKI.
 * Sa dvije instance backenda (V7, docker-compose --scale) korisnik bi sa
 * in-memory brojacem dobio dupli limit - svaka instanca broji za sebe.
 */
@Configuration
public class RateLimitConfig {

    /**
     * ProxyManager = "fabrika kanti": po kljucu (String) daje kantu cije
     * stanje zivi u Redisu, ne u JVM-u.
     */
    @Bean
    public ProxyManager<String> rateLimitProxyManager(LettuceConnectionFactory factory) {
        // Koristimo isti Lettuce klijent koji Spring vec drzi za kes, samo
        // otvaramo jos jednu konekciju sa drugim kodekom: Bucket4j hoce
        // vrijednost kao byte[] (svoj binarni format), a kljuc ostavljamo
        // kao String da se u redis-cli vidi "ratelimit:tickets:marko@..."
        RedisClient client = (RedisClient) factory.getRequiredNativeClient();
        StatefulRedisConnection<String, byte[]> connection =
                client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return Bucket4jLettuce.casBasedBuilder(connection)
                // Kljuc u Redisu umire sam onog trenutka kad bi kanta ionako
                // bila puna - inace bi Redis zauvijek pamtio svakog korisnika
                // koji je ikad uzeo broj.
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();
    }
}
