package com.queue.backend.common.idempotency;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Jedan red = "potrosac X je obradio event Y". Vidi IdempotencyGuard. */
@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(length = 120)
    private String id;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    public ProcessedEvent(String id) {
        this.id = id;
    }
}
