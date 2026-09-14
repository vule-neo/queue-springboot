package com.queue.backend.tickets;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Ime metode je upit: red + dan, poredano po rednom broju.
    // "QueueId" ulazi u povezani objekat (ticket.queue.id).
    List<Ticket> findByQueueIdAndIssuedDateOrderBySequenceNoAsc(Long queueId, LocalDate issuedDate);

    // "First" = LIMIT 1. Sljedeci na redu: najmanji sequenceNo medju onima
    // koji jos cekaju. Ovo je upit koji ce V4 morati da zakljuca.
    Optional<Ticket> findFirstByQueueIdAndIssuedDateAndStatusOrderBySequenceNoAsc(
            Long queueId, LocalDate issuedDate, TicketStatus status);
}
