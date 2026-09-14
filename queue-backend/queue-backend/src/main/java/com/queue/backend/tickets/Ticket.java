package com.queue.backend.tickets;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.queue.backend.queues.Queue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_id", nullable = false)
    private Queue queue;

    // Ono sto covjek vidi na papiru: "A007".
    @Column(nullable = false, length = 10)
    private String number;

    // Redni broj unutar reda za taj dan - po njemu ide sortiranje.
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    // STRING obavezno: default je ORDINAL, koji upisuje broj u kolonu
    // i pao bi na CHECK constraintu.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Nullable: popunjavaju se kroz zivotni ciklus, nose V6 statistiku.
    @Column(name = "called_at")
    private OffsetDateTime calledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
