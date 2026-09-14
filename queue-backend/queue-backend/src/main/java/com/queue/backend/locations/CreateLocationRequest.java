package com.queue.backend.locations;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLocationRequest(

        @NotNull Long organizationId,

        @NotBlank @Size(max = 120) String name,

        @Size(max = 255) String address) {
}
