package com.queue.backend.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ulaz za /auth/register i /auth/login.
 * Kad se register i login razidju (npr. register dobije ime ili ulogu),
 * razdvoji ih u dva zasebna recorda.
 */
public record UserRequest(

        @NotBlank @Email @Size(max = 160) String email,

        @NotBlank @Size(min = 8, max = 72) String password) {
}
