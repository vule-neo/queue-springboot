package com.queue.backend.queues;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record QueueResponse(
        Long id,
        Long locationId,
        Long serviceId,
        String prefix,
        boolean open,
        int lastNumber,
        LocalDate lastNumberDate,
        OffsetDateTime createdAt) {

    public static QueueResponse from(Queue q) {
        return new QueueResponse(
                q.getId(),
                q.getLocation().getId(),
                q.getServiceType().getId(),
                q.getPrefix(),
                q.isOpen(),
                q.getLastNumber(),
                q.getLastNumberDate(),
                q.getCreatedAt());
    }
}
