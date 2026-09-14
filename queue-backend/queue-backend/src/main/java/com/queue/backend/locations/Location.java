package com.queue.backend.locations;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.queue.backend.organizations.Organization;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY jer je EAGER default kod @ManyToOne: bez ovoga bi svako
    // ucitavanje lokacije povuklo i organizaciju, uvijek.
    // optional=false je Hibernate-u nagovjestaj da FK nije nullable.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
