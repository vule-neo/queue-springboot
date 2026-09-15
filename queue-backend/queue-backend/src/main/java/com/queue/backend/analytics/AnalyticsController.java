package com.queue.backend.analytics;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService service;
    private final ZoneId zone;

    public AnalyticsController(AnalyticsService service, @Value("${app.timezone}") String zone) {
        this.service = service;
        this.zone = ZoneId.of(zone);
    }

    // ---------- menadzer ----------

    /** Svi redovi danas (ili ?date=2026-09-14 za drugi dan). */
    @GetMapping("/api/analytics/today")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<QueueStatsResponse> today(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.statsForDay(date != null ? date : LocalDate.now(zone));
    }

    @GetMapping("/api/analytics/queues/{queueId}/hourly")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<HourlyResponse> hourly(
            @PathVariable Long queueId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.issuedPerHour(queueId, date != null ? date : LocalDate.now(zone));
    }

    // ---------- musterija ----------

    /** Koliko jos da cekam? Svaki prijavljeni - nema privatnih podataka. */
    @GetMapping("/api/tickets/{id}/estimate")
    public WaitEstimateResponse estimate(@PathVariable Long id) {
        return service.estimate(id);
    }
}
