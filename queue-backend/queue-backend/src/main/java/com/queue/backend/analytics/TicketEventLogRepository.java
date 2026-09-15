package com.queue.backend.analytics;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventLogRepository extends JpaRepository<TicketEventLog, Long> {

    Optional<TicketEventLog> findByEventId(String eventId);

    long countByEventId(String eventId);
}
