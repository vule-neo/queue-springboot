package com.queue.backend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.queues.Queue;
import com.queue.backend.tickets.Ticket;
import com.queue.backend.tickets.TicketRepository;
import com.queue.backend.tickets.TicketStatus;

/**
 * Unit test procjene: repozitoriji su mockovi, pa testiramo samo formulu
 * i izbor izvora (istorija vs konstanta), bez baze.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService.estimate")
class AnalyticsServiceTest {

    private static final long DEFAULT = 300;

    @Mock AnalyticsRepository analytics;
    @Mock TicketRepository tickets;

    AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(analytics, tickets, "Europe/Vienna", DEFAULT, 7);
    }

    @Test
    @DisplayName("red ima istoriju: ispred x prosjek iz istorije, basis HISTORY")
    void saIstorijom() {
        when(tickets.findById(10L)).thenReturn(Optional.of(ticket(10L, 5, TicketStatus.WAITING)));
        when(tickets.countByQueueIdAndIssuedDateAndStatusAndSequenceNoLessThan(
                eq(1L), any(LocalDate.class), eq(TicketStatus.WAITING), eq(5))).thenReturn(3L);
        when(analytics.avgServiceSecondsSince(eq(1L), any(LocalDate.class))).thenReturn(120.4);

        WaitEstimateResponse r = service.estimate(10L);

        assertThat(r.ahead()).isEqualTo(3);
        assertThat(r.avgServiceSeconds()).isEqualTo(120);
        assertThat(r.estimatedWaitSeconds()).isEqualTo(360);
        assertThat(r.basis()).isEqualTo(WaitEstimateResponse.Basis.HISTORY);
    }

    @Test
    @DisplayName("red bez istorije: konstanta, basis DEFAULT")
    void bezIstorije() {
        when(tickets.findById(10L)).thenReturn(Optional.of(ticket(10L, 5, TicketStatus.WAITING)));
        when(tickets.countByQueueIdAndIssuedDateAndStatusAndSequenceNoLessThan(
                anyLong(), any(LocalDate.class), any(TicketStatus.class), anyInt())).thenReturn(2L);
        when(analytics.avgServiceSecondsSince(anyLong(), any(LocalDate.class))).thenReturn(null);

        WaitEstimateResponse r = service.estimate(10L);

        assertThat(r.estimatedWaitSeconds()).isEqualTo(2 * DEFAULT);
        assertThat(r.basis()).isEqualTo(WaitEstimateResponse.Basis.DEFAULT);
    }

    @Test
    @DisplayName("ticket koji vise ne ceka: 0 ispred, 0 cekanja")
    void neCeka() {
        when(tickets.findById(10L)).thenReturn(Optional.of(ticket(10L, 5, TicketStatus.CALLED)));
        when(analytics.avgServiceSecondsSince(anyLong(), any(LocalDate.class))).thenReturn(60.0);

        WaitEstimateResponse r = service.estimate(10L);

        assertThat(r.ahead()).isZero();
        assertThat(r.estimatedWaitSeconds()).isZero();
        // Brojanje ispred se ni ne poziva - Mockito strict stubs bi se bunio da je stubovano.
    }

    @Test
    void nepostojeciTicket() {
        when(tickets.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.estimate(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("issuedPerHour vraca svih 24 sata, rupe popunjene nulom")
    void svih24Sata() {
        AnalyticsRepository.HourlyView h9 = hourly(9, 4);
        AnalyticsRepository.HourlyView h14 = hourly(14, 1);
        when(analytics.issuedPerHour(eq(1L), any(), any(), eq("Europe/Vienna"))).thenReturn(List.of(h9, h14));

        List<HourlyResponse> r = service.issuedPerHour(1L, LocalDate.of(2026, 9, 15));

        assertThat(r).hasSize(24);
        assertThat(r.get(9).issued()).isEqualTo(4);
        assertThat(r.get(14).issued()).isEqualTo(1);
        assertThat(r.get(0).issued()).isZero();
        assertThat(r.get(23).issued()).isZero();
    }

    // ---------------------------------------------------------------

    private static Ticket ticket(Long id, int seq, TicketStatus status) {
        Queue q = new Queue();
        q.setId(1L);
        Ticket t = new Ticket();
        t.setId(id);
        t.setQueue(q);
        t.setNumber("A00" + seq);
        t.setSequenceNo(seq);
        t.setIssuedDate(LocalDate.now());
        t.setStatus(status);
        return t;
    }

    private static AnalyticsRepository.HourlyView hourly(int hour, long issued) {
        return new AnalyticsRepository.HourlyView() {
            public int getHour() { return hour; }
            public long getIssued() { return issued; }
        };
    }
}
