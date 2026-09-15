package com.queue.backend.analytics;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.queue.backend.tickets.TicketEvent;
import com.queue.backend.tickets.TicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Jedan red po dogadjaju - sirovina za V6 statistiku.
 *
 * Bez @ManyToOne na Ticket, namjerno: statistika je istorija i ne smije
 * zavisiti od toga postoji li ticket jos. Kopiramo ono sto nam treba.
 */
@Entity
@Table(name = "ticket_event_log")
@Getter
@NoArgsConstructor
public class TicketEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private TicketEvent.Tip eventType;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "queue_id", nullable = false)
    private Long queueId;

    @Column(name = "ticket_number", nullable = false, length = 10)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    public static TicketEventLog from(TicketEvent e) {
        TicketEventLog log = new TicketEventLog();
        log.eventId = e.eventId();
        log.eventType = e.tip();
        log.ticketId = e.ticket().id();
        log.queueId = e.queueId();
        log.ticketNumber = e.ticket().number();
        log.status = e.ticket().status();
        log.occurredAt = e.kada();
        return log;
    }
}
