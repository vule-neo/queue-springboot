package com.queue.backend.tickets;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TicketResponse(
        Long id,
        Long queueId,
        String number,
        int sequenceNo,
        LocalDate issuedDate,
        TicketStatus status,
        OffsetDateTime createdAt,
        // Null dok se ne dogode. Razlika createdAt -> calledAt je cekanje,
        // calledAt -> completedAt je trajanje obrade. To je ulaz za V6.
        OffsetDateTime calledAt,
        OffsetDateTime completedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getQueue().getId(),
                t.getNumber(),
                t.getSequenceNo(),
                t.getIssuedDate(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getCalledAt(),
                t.getCompletedAt());
    }
}
