package com.queue.backend.analytics;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Agregacije za dashboard. Nativni SQL, ne JPQL: JPQL ne zna FILTER ni
 * EXTRACT(EPOCH ...), a projekat je ionako Postgres-only (Flyway skripte).
 *
 * Repository<> (ne JpaRepository): nema save/delete - ovdje se samo cita.
 * Rezultati su interface projekcije: Spring pravi proxy koji cita kolone
 * po imenu aliasa, pa ne treba entitet za nesto sto nije entitet.
 */
public interface AnalyticsRepository extends Repository<TicketEventLog, Long> {

    /**
     * Stanje svih redova za jedan dan. LEFT JOIN da se vidi i red bez
     * ijednog ticketa (sve nule), a ne da ga nema u listi.
     *
     * COUNT(*) FILTER (WHERE ...) = "prebroj samo one koji zadovoljavaju"
     * u jednom prolazu, umjesto sedam odvojenih upita.
     *
     * Cekanje = created_at -> called_at. Obrada = called_at -> completed_at
     * (nemamo serving_at, pa obrada ukljucuje i vrijeme dok musterija
     * prilazi salteru - svjesna aproksimacija).
     */
    @Query(value = """
            SELECT q.id                                                       AS queueId,
                   q.prefix                                                   AS prefix,
                   COUNT(t.id)                                                AS issued,
                   COUNT(*) FILTER (WHERE t.status = 'WAITING')               AS waiting,
                   COUNT(*) FILTER (WHERE t.status IN ('CALLED', 'SERVING'))  AS inProgress,
                   COUNT(*) FILTER (WHERE t.status = 'COMPLETED')             AS completed,
                   COUNT(*) FILTER (WHERE t.status = 'CANCELLED')             AS cancelled,
                   COUNT(*) FILTER (WHERE t.status = 'NO_SHOW')               AS noShow,
                   AVG(EXTRACT(EPOCH FROM (t.called_at - t.created_at)))      AS avgWaitSeconds,
                   AVG(EXTRACT(EPOCH FROM (t.completed_at - t.called_at)))
                       FILTER (WHERE t.status = 'COMPLETED')                  AS avgServiceSeconds
            FROM queue q
            LEFT JOIN ticket t ON t.queue_id = q.id AND t.issued_date = :day
            GROUP BY q.id, q.prefix
            ORDER BY q.id
            """, nativeQuery = true)
    List<QueueStatsView> statsForDay(@Param("day") LocalDate day);

    /**
     * Koliko je brojeva izdato po satu - za grafikon. Iz ticket_event_log,
     * ne iz ticket: log je istorija koja ostaje i kad se ticketi jednom
     * budu arhivirali.
     */
    @Query(value = """
            SELECT EXTRACT(HOUR FROM (e.occurred_at AT TIME ZONE :tz))::int AS hour,
                   COUNT(*)                                                 AS issued
            FROM ticket_event_log e
            WHERE e.queue_id = :queueId
              AND e.event_type = 'IZDAT'
              AND e.occurred_at >= :from AND e.occurred_at < :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<HourlyView> issuedPerHour(@Param("queueId") Long queueId,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to,
                                   @Param("tz") String tz);

    /**
     * Prosjecna obrada u redu kroz istoriju - ulaz za procjenu cekanja V2.
     * Null ako nema nijednog zavrsenog ticketa u periodu.
     */
    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (t.completed_at - t.called_at)))
            FROM ticket t
            WHERE t.queue_id = :queueId
              AND t.status = 'COMPLETED'
              AND t.issued_date >= :from
            """, nativeQuery = true)
    Double avgServiceSecondsSince(@Param("queueId") Long queueId, @Param("from") LocalDate from);

    interface QueueStatsView {
        Long getQueueId();
        String getPrefix();
        long getIssued();
        long getWaiting();
        long getInProgress();
        long getCompleted();
        long getCancelled();
        long getNoShow();
        Double getAvgWaitSeconds();
        Double getAvgServiceSeconds();
    }

    interface HourlyView {
        int getHour();
        long getIssued();
    }
}
