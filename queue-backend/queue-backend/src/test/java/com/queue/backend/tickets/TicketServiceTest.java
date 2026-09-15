package com.queue.backend.tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.queue.backend.common.InvalidStateTransitionException;
import com.queue.backend.common.NotFoundException;
import com.queue.backend.queues.Queue;
import com.queue.backend.queues.QueueService;
import com.queue.backend.users.Role;
import com.queue.backend.users.User;
import com.queue.backend.users.UserRepository;

/**
 * Unit testovi servisa: baza, Rabbit, Redis su mockovi. Ovdje se testira
 * LOGIKA - state machine, brojac, vlasnistvo - a ne infrastruktura.
 * Infrastrukturu testiraju integracioni testovi (AbstractIntegrationTest).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService")
class TicketServiceTest {

    @Mock TicketRepository repository;
    @Mock QueueService queueService;
    @Mock TicketEventPublisher events;
    @Mock QueueTicketsCache cache;
    @Mock UserRepository users;

    @InjectMocks TicketService service;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("prvi broj dana: brojac se resetuje, broj je PREFIX001")
        void prviBrojDana() {
            Queue red = red(7, LocalDate.now().minusDays(1));   // juce je bilo 7
            when(queueService.getEntityForUpdate(1L)).thenReturn(red);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TicketResponse r = service.create(1L, null);

            assertThat(r.number()).isEqualTo("A001");
            assertThat(r.sequenceNo()).isEqualTo(1);
            assertThat(red.getLastNumber()).isEqualTo(1);
            assertThat(red.getLastNumberDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("isti dan: brojac raste")
        void istiDan() {
            Queue red = red(7, LocalDate.now());
            when(queueService.getEntityForUpdate(1L)).thenReturn(red);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.create(1L, null).number()).isEqualTo("A008");
        }

        @Test
        @DisplayName("status WAITING, vlasnik iz emaila, event objavljen, kes obrisan")
        void sporedniEfekti() {
            Queue red = red(0, null);
            User marko = user("marko@test.com");
            when(queueService.getEntityForUpdate(1L)).thenReturn(red);
            when(users.findByEmail("marko@test.com")).thenReturn(Optional.of(marko));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(1L, "marko@test.com");

            ArgumentCaptor<Ticket> snimljen = ArgumentCaptor.forClass(Ticket.class);
            verify(repository).save(snimljen.capture());
            assertThat(snimljen.getValue().getStatus()).isEqualTo(TicketStatus.WAITING);
            assertThat(snimljen.getValue().getUser()).isSameAs(marko);

            verify(events).objavi(any(TicketEvent.class));
            verify(cache).evictAfterCommit(1L);
        }

        @Test
        @DisplayName("nepostojeci red: NotFound, nista se ne snima")
        void nepostojeciRed() {
            when(queueService.getEntityForUpdate(99L)).thenThrow(new NotFoundException("Red 99 ne postoji"));

            assertThatThrownBy(() -> service.create(99L, null)).isInstanceOf(NotFoundException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("state machine")
    class StateMachine {

        @Test
        @DisplayName("WAITING -> CALLED postavlja calledAt")
        void pozivanje() {
            Ticket t = ticket(TicketStatus.WAITING);
            when(repository.findById(5L)).thenReturn(Optional.of(t));

            TicketResponse r = service.call(5L);

            assertThat(r.status()).isEqualTo(TicketStatus.CALLED);
            assertThat(t.getCalledAt()).isNotNull();
            verify(events).objavi(any());
        }

        @Test
        @DisplayName("WAITING -> COMPLETED nije dozvoljen (preskakanje stanja)")
        void preskakanje() {
            when(repository.findById(5L)).thenReturn(Optional.of(ticket(TicketStatus.WAITING)));

            assertThatThrownBy(() -> service.complete(5L))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("WAITING -> COMPLETED");
            verify(events, never()).objavi(any());
        }

        @Test
        @DisplayName("COMPLETED je zavrsno - nema nazad")
        void zavrsnoStanje() {
            when(repository.findById(5L)).thenReturn(Optional.of(ticket(TicketStatus.COMPLETED)));

            assertThatThrownBy(() -> service.requeue(5L)).isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("SKIPPED -> WAITING -> CALLED: calledAt se ne prepisuje")
        void calledAtSamoPrviPut() {
            Ticket t = ticket(TicketStatus.WAITING);
            when(repository.findById(5L)).thenReturn(Optional.of(t));

            service.call(5L);
            var prviPoziv = t.getCalledAt();
            service.skip(5L);
            service.requeue(5L);
            service.call(5L);

            assertThat(t.getCalledAt()).isSameAs(prviPoziv);
        }
    }

    @Nested
    @DisplayName("cancel - vlasnistvo")
    class Cancel {

        @Test
        @DisplayName("svoj ticket: moze")
        void svoj() {
            Ticket t = ticket(TicketStatus.WAITING);
            t.setUser(user("ana@test.com"));
            when(repository.findById(5L)).thenReturn(Optional.of(t));

            assertThat(service.cancel(5L, "ana@test.com", false).status()).isEqualTo(TicketStatus.CANCELLED);
        }

        @Test
        @DisplayName("tudji ticket: 403, status ostaje")
        void tudji() {
            Ticket t = ticket(TicketStatus.WAITING);
            t.setUser(user("ana@test.com"));
            when(repository.findById(5L)).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.cancel(5L, "marko@test.com", false))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(t.getStatus()).isEqualTo(TicketStatus.WAITING);
            verify(events, never()).objavi(any());
        }

        @Test
        @DisplayName("tudji ticket, ali osoblje: moze")
        void osoblje() {
            Ticket t = ticket(TicketStatus.WAITING);
            t.setUser(user("ana@test.com"));
            when(repository.findById(5L)).thenReturn(Optional.of(t));

            assertThat(service.cancel(5L, "salter@test.com", true).status()).isEqualTo(TicketStatus.CANCELLED);
        }

        @Test
        @DisplayName("ticket bez vlasnika (salter ga izdao): svako moze")
        void bezVlasnika() {
            when(repository.findById(5L)).thenReturn(Optional.of(ticket(TicketStatus.WAITING)));

            assertThat(service.cancel(5L, "bilo@ko.com", false).status()).isEqualTo(TicketStatus.CANCELLED);
        }
    }

    // ---------------------------------------------------------------

    private static Queue red(int lastNumber, LocalDate lastNumberDate) {
        Queue q = new Queue();
        q.setId(1L);
        q.setPrefix("A");
        q.setLastNumber(lastNumber);
        q.setLastNumberDate(lastNumberDate);
        return q;
    }

    private static Ticket ticket(TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(5L);
        t.setQueue(red(3, LocalDate.now()));
        t.setNumber("A003");
        t.setSequenceNo(3);
        t.setIssuedDate(LocalDate.now());
        t.setStatus(status);
        return t;
    }

    private static User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setRole(Role.CUSTOMER);
        return u;
    }
}
