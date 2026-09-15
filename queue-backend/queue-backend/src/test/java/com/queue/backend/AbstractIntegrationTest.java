package com.queue.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Osnova za sve integracione testove: podize PRAVI Postgres, Redis i
 * RabbitMQ u Dockeru, jednom po JVM-u.
 *
 * Zasto ne H2: H2 nije Postgres. FILTER (WHERE ...), EXTRACT(EPOCH ...),
 * FOR UPDATE semantika, timestamptz - sve to bi na H2 ili puklo ili tiho
 * radilo drugacije. Test koji prolazi na H2 a pada u produkciji je gori
 * od nikakvog testa.
 *
 * Singleton pattern (start u static bloku, bez @Container): @Container bi
 * gasio kontejnere poslije svake test klase, a Spring kesira kontekst
 * izmedju klasa - kontekst bi pokazivao na mrtav kontejner. Ovako zive
 * do kraja JVM-a, Testcontainers ih sam pocisti (Ryuk).
 *
 * @ServiceConnection: Boot sam procita host/port/lozinku iz kontejnera
 * i prepise spring.datasource.*, spring.data.redis.*, spring.rabbitmq.*.
 * Nema @DynamicPropertySource rucnog prepisivanja.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    // GenericContainer, ne posebna klasa: Boot prepoznaje Redis po imenu image-a.
    @ServiceConnection
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    static {
        POSTGRES.start();
        REDIS.start();
        RABBIT.start();
    }
}
