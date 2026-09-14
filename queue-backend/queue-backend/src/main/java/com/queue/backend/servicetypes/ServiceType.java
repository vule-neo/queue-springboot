package com.queue.backend.servicetypes;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.queue.backend.organizations.Organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usluga iz kataloga organizacije ("vadjenje krvi", "uplata").
 * Klasa se ne zove Service da se ne sudara sa @Service anotacijom;
 * tabela u bazi ostaje "service".
 */
@Entity
@Table(name = "service")
@Getter
@Setter
@NoArgsConstructor
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    // Procjena trajanja, ulaz u racun cekanja u V6.
    @Column(name = "avg_duration_minutes", nullable = false)
    private int avgDurationMinutes = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
