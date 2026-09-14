package com.queue.backend.users;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Klasa User, tabela app_user - "user" je rezervisana rijec u Postgresu.
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    // Polje se zove passwordHash, ne password - da se nikad ne pomijesa
    // sa cistom lozinkom. Mapira se na kolonu password_hash.
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // STRING obavezno - ORDINAL bi upisao broj i pao na ck_app_user_role.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
