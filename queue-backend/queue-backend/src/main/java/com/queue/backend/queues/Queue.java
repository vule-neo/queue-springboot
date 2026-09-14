package com.queue.backend.queues;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.queue.backend.locations.Location;
import com.queue.backend.servicetypes.ServiceType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ziva instanca usluge na lokaciji - ono u sta se ljudi stvarno redaju.
 * Pazi na import: java.util.Queue je druga stvar.
 */
@Entity
@Table(name = "queue")
@Getter
@Setter
@NoArgsConstructor
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceType serviceType;

    // Slovo na ticketu: A -> A001, A002...
    @Column(nullable = false, length = 3)
    private String prefix;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    // Brojac zadnjeg izdatog broja za lastNumberDate.
    // Ovo je tacka nadmetanja koju V4 zakljucava.
    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    // Dan za koji lastNumber vazi. Drugi datum -> brojac krece od nule.
    @Column(name = "last_number_date")
    private LocalDate lastNumberDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
