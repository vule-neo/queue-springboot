package com.queue.backend.queues;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQueueRequest(

        @NotNull Long locationId,

        @NotNull Long serviceId,

        @NotBlank @Size(max = 3) String prefix) {
}
