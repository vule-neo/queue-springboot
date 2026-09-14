package com.queue.backend.users;

import java.time.OffsetDateTime;

/**
 * NIKAD ne sadrzi passwordHash. DTO postoji upravo zato da hash
 * ne moze slucajno izaci iz aplikacije.
 */
public record UserResponse(
        Long id,
        String email,
        String role,
        OffsetDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.getCreatedAt());
    }
}
