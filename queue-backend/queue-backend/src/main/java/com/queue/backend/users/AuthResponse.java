package com.queue.backend.users;

/**
 * Odgovor na uspjesan login. "Bearer" je tip tokena - klijent ga salje kao
 * Authorization: Bearer <token>
 */
public record AuthResponse(String token, String type, String email, String role) {

    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getRole().name());
    }
}
