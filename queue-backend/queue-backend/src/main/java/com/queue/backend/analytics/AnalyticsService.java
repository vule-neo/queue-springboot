package com.queue.backend.analytics;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.tickets.Ticket;
import com.queue.backend.tickets.TicketRepository;
import com.queue.backend.tickets.TicketStatus;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analytics;
    private final TicketRepository tickets;
    private final ZoneId zone;
    private final long defaultServiceSeconds;
    private final int historyDays;

    public AnalyticsService(AnalyticsRepository analytics,
                            TicketRepository tickets,
                            @Value("${app.timezone}") String zone,
                            @Value("${app.estimate.default-service-seconds}") long defaultServiceSeconds,
                            @Value("${app.estimate.history-days}") int historyDays) {
        this.analytics = analytics;
        this.tickets = tickets;
        this.zone = ZoneId.of(zone);
        this.defaultServiceSeconds = defaultServiceSeconds;
        this.historyDays = historyDays;
    }

    // ---------------------------------------------------------------
    // Dashboard
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<QueueStatsResponse> statsForDay(LocalDate day) {
        return analytics.statsForDay(day).stream()
                .map(v -> QueueStatsResponse.from(v, day))
                .toList();
    }

    /**
     * Uvijek 24 sata, i oni sa nulom: SQL vraca samo sate koji imaju
     * redova, a grafikon hoce punu osu.
     */
    @Transactional(readOnly = true)
    public List<HourlyResponse> issuedPerHour(Long queueId, LocalDate day) {
        // Dan u NASOJ zoni, ne UTC: 00:00 u Becu nije 00:00 u bazi (timestamptz).
        ZonedDateTime from = day.atStartOfDay(zone);
        ZonedDateTime to = from.plusDays(1);

        Map<Integer, Long> poSatu = new HashMap<>();
        analytics.issuedPerHour(queueId, from.toOffsetDateTime(), to.toOffsetDateTime(), zone.getId())
                .forEach(v -> poSatu.put(v.getHour(), v.getIssued()));

        return IntStream.range(0, 24)
                .mapToObj(h -> new HourlyResponse(h, poSatu.getOrDefault(h, 0L)))
                .toList();
    }

    // ---------------------------------------------------------------
    // Procjena cekanja
    // ---------------------------------------------------------------

    /**
     * procjena = (koliko ih ceka ispred) x (prosjecna obrada)
     *
     * V1 je bila konstanta za obradu. V2 (ovo): prosjek iz zavrsenih
     * ticketa istog reda u zadnjih N dana, a konstanta samo dok red
     * nema istoriju. Jos uvijek gruba - ne zna koliko saltera radi
     * (nemamo counter modul), pa pretpostavlja jedan.
     */
    @Transactional(readOnly = true)
    public WaitEstimateResponse estimate(Long ticketId) {
        Ticket t = tickets.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " ne postoji"));

        long ahead = 0;
        if (t.getStatus() == TicketStatus.WAITING) {
            ahead = tickets.countByQueueIdAndIssuedDateAndStatusAndSequenceNoLessThan(
                    t.getQueue().getId(), t.getIssuedDate(), TicketStatus.WAITING, t.getSequenceNo());
        }

        Double istorija = analytics.avgServiceSecondsSince(
                t.getQueue().getId(), LocalDate.now(zone).minusDays(historyDays));

        long obrada;
        WaitEstimateResponse.Basis basis;
        if (istorija != null && istorija > 0) {
            obrada = Math.round(istorija);
            basis = WaitEstimateResponse.Basis.HISTORY;
        } else {
            obrada = defaultServiceSeconds;
            basis = WaitEstimateResponse.Basis.DEFAULT;
        }

        return new WaitEstimateResponse(t.getId(), t.getNumber(), t.getStatus(),
                ahead, obrada, ahead * obrada, basis);
    }
}
