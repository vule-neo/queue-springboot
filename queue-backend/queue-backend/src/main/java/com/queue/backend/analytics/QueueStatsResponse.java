package com.queue.backend.analytics;

import java.time.LocalDate;

/** Jedan red, jedan dan. Prosjeci su null dok nema podataka (0 bi lagalo). */
public record QueueStatsResponse(
        Long queueId,
        String prefix,
        LocalDate day,
        long issued,
        long waiting,
        long inProgress,
        long completed,
        long cancelled,
        long noShow,
        Double avgWaitSeconds,
        Double avgServiceSeconds) {

    static QueueStatsResponse from(AnalyticsRepository.QueueStatsView v, LocalDate day) {
        return new QueueStatsResponse(v.getQueueId(), v.getPrefix(), day,
                v.getIssued(), v.getWaiting(), v.getInProgress(), v.getCompleted(),
                v.getCancelled(), v.getNoShow(), v.getAvgWaitSeconds(), v.getAvgServiceSeconds());
    }
}
