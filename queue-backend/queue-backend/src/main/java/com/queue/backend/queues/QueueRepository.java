package com.queue.backend.queues;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface QueueRepository extends JpaRepository<Queue, Long> {

    /**
     * Dohvat reda sa zakljucavanjem tog jednog reda u bazi.
     *
     * PESSIMISTIC_WRITE se prevede u "SELECT ... FOR UPDATE". Postgres
     * zakljuca taj red dok transakcija ne zavrsi; druga nit koja trazi
     * isti red CEKA umjesto da radi sa zastarjelom vrijednoscu.
     *
     * Ovo je jedina tacka nadmetanja u sistemu - brojac lastNumber.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Queue> findWithLockById(Long id);
}
