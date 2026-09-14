package com.queue.backend.servicetypes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateServiceTypeRequest(

        @NotNull Long organizationId,

        @NotBlank @Size(max = 120) String name,

        // @Positive pokriva isto sto i ck_service_duration u bazi -
        // ali vraca cistu 400 poruku umjesto constraint greske.
        @NotNull @Positive Integer avgDurationMinutes) {
}
