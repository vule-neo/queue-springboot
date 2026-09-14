package com.queue.backend.organizations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Samo ono sto klijent SMIJE poslati - nema id ni createdAt.
 */
public record CreateOrganizationRequest(

        @NotBlank @Size(max = 120) String name,

        @NotBlank @Size(max = 60) String slug) {
}
