package com.queue.backend.tickets;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.InvalidStateTransitionException;
import com.queue.backend.common.NotFoundException;
import com.queue.backend.config.CacheConfig;
import com.queue.backend.queues.Queue;
import com.queue.backend.queues.QueueService;
import com.queue.backend.users.User;
import com.queue.backend.users.UserRepository;

@Service
public class TicketService {

    private final TicketRepository repository;
    private final QueueService queueService;
    private final TicketEventPublisher events;
    private final QueueTicketsCache cache;
    private final UserRepository users;

    public TicketService(TicketRepository repository,
                         QueueService queueService,
                         TicketEventPublisher events,
                         QueueTicketsCache cache,
                         UserRepository users) {
        this.repository = repository;
        this.queueService = queueService;
        this.events = events;
        this.cache = cache;
        this.users = users;
    }

    // ---------------------------------------------------------------
    // Izdavanje
    // ---------------------------------------------------------------

    /**
     * Izdaje sljedeci broj u redu.
     *
     * Brojac lastNumber je tacka nadmetanja: citanje pa upis nije atomicno.
     * Bez lockinga su dvije od tri musterije dobijale gresku umjesto broja
     * (vidi DOKAZ-race-condition.md). Zato red zakljucavamo prvi.
     */
    /**
     * @param email ko uzima broj (iz tokena); null = bez vlasnika
     *              (salter izdaje za nekoga bez naloga).
     */
    @Transactional
    public TicketResponse create(Long queueId, String email) {
        // FOR UPDATE: sve ostale niti koje traze OVAJ red cekaju dok
        // ova transakcija ne zavrsi. Ostali redovi nisu zakljucani -
        // dom zdravlja i banka se ne blokiraju medjusobno.
        Queue queue = queueService.getEntityForUpdate(queueId);

        LocalDate danas = LocalDate.now();

        if (!danas.equals(queue.getLastNumberDate())) {
            queue.setLastNumber(0);
            queue.setLastNumberDate(danas);
        }

        int sljedeci = queue.getLastNumber() + 1;
        queue.setLastNumber(sljedeci);
        // queue se ne snima rucno - dirty checking posalje UPDATE na kraju.

        Ticket ticket = new Ticket();
        ticket.setQueue(queue);
        ticket.setNumber(String.format("%s%03d", queue.getPrefix(), sljedeci));
        ticket.setSequenceNo(sljedeci);
        ticket.setIssuedDate(danas);
        ticket.setStatus(TicketStatus.WAITING);
        if (email != null) {
            // orElse(null), ne orElseThrow: token je vec validiran u filteru,
            // a i bez vlasnika ticket je ispravan.
            ticket.setUser(users.findByEmail(email).orElse(null));
        }

        Ticket snimljen = repository.save(ticket);

        // Broadcast ide iz SERVISA, ne iz kontrolera: tako poruka ode i kad
        // promjena dodje iz WebSocket-a, Rabbit listenera (V5) ili zakazanog
        // posla - a ne samo iz HTTP zahtjeva.
        events.objavi(TicketEvent.izdat(snimljen));
        cache.evictAfterCommit(queueId);

        return TicketResponse.from(snimljen);
    }

    // ---------------------------------------------------------------
    // Zivotni ciklus
    // ---------------------------------------------------------------

    /**
     * Pozovi sljedeceg koji ceka u ovom redu danas.
     *
     * Zakljucavamo RED, ne ticket. Dva razloga:
     *  1) time se svi pozivi za taj red poredaju u niz, pa dva saltera
     *     ne mogu procitati isti ticket (prije popravke je 20 saltera
     *     pozvalo samo 6 razlicitih ljudi)
     *  2) i create() zakljucava red prvi - isti redoslijed zakljucavanja
     *     svuda znaci da deadlock nije moguc
     */
    @Transactional
    public TicketResponse callNext(Long queueId) {
        // Ujedno i provjera postojanja: 404 "red ne postoji" nije isto
        // sto i 404 "red je prazan".
        queueService.getEntityForUpdate(queueId);

        Ticket sljedeci = repository
                .findFirstByQueueIdAndIssuedDateAndStatusOrderBySequenceNoAsc(
                        queueId, LocalDate.now(), TicketStatus.WAITING)
                .orElseThrow(() -> new NotFoundException("Nema nikoga u redu " + queueId));

        return promijeni(sljedeci, TicketStatus.CALLED);
    }

    /** Pozovi konkretan ticket (npr. preskoceni koji se vratio). */
    @Transactional
    public TicketResponse call(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.CALLED);
    }

    /** Musterija je dosla na salter - pocinje obrada. */
    @Transactional
    public TicketResponse startServing(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.SERVING);
    }

    @Transactional
    public TicketResponse complete(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.COMPLETED);
    }

    /** Pozvan pa se nije pojavio. */
    @Transactional
    public TicketResponse noShow(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.NO_SHOW);
    }

    /** Preskoci - ticket ceka da ga se vrati u red. */
    @Transactional
    public TicketResponse skip(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.SKIPPED);
    }

    /** Vrati preskoceni ticket u red. */
    @Transactional
    public TicketResponse requeue(Long ticketId) {
        return promijeni(getEntity(ticketId), TicketStatus.WAITING);
    }

    /**
     * Musterija odustaje.
     *
     * Provjera vlasnistva je OVDJE, ne u kontroleru: pravilo "otkazujes
     * samo svoj" je poslovno pravilo i mora vaziti odakle god poziv dosao.
     *
     * @param email ko trazi otkazivanje
     * @param osoblje osoblje smije otkazati bilo ciji (musterija je
     *                odustala na salteru, papir je izgubljen...)
     */
    @Transactional
    public TicketResponse cancel(Long ticketId, String email, boolean osoblje) {
        Ticket ticket = getEntity(ticketId);

        User vlasnik = ticket.getUser();
        boolean tudji = vlasnik != null && !vlasnik.getEmail().equals(email);
        if (tudji && !osoblje) {
            // AccessDeniedException -> 403 kroz GlobalExceptionHandler.
            // Ne otkrivamo ciji je - samo da nije tvoj.
            throw new AccessDeniedException("Ticket " + ticket.getNumber() + " nije tvoj");
        }

        return promijeni(ticket, TicketStatus.CANCELLED);
    }

    // ---------------------------------------------------------------
    // Citanje
    // ---------------------------------------------------------------

    /**
     * Jedini kesirani upit: gadja ga javni ekran bez tokena, odgovor je isti
     * za sve, a mijenja se samo kroz create() i promijeni() - obje brisu
     * unos. Kljuc je queueId, pa promjena u jednom redu ne dira ostale.
     */
    @Cacheable(cacheNames = CacheConfig.QUEUE_TICKETS, key = "#queueId")
    @Transactional(readOnly = true)
    public List<TicketResponse> findByQueue(Long queueId) {
        return repository
                .findByQueueIdAndIssuedDateOrderBySequenceNoAsc(queueId, LocalDate.now())
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return TicketResponse.from(getEntity(id));
    }

    // ---------------------------------------------------------------

    private Ticket getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket " + id + " ne postoji"));
    }

    /**
     * Jedina tacka gdje se status mijenja. Svaki prelaz prolazi ovuda,
     * pa se pravilo ne moze zaobici - ni iz kontrolera, ni iz WebSocket-a
     * u V3, ni iz Rabbit listenera u V5.
     */
    private TicketResponse promijeni(Ticket ticket, TicketStatus cilj) {
        TicketStatus trenutni = ticket.getStatus();

        if (!trenutni.moze(cilj)) {
            throw new InvalidStateTransitionException(
                    "Ticket " + ticket.getNumber() + ": prelaz " + trenutni + " -> " + cilj
                            + " nije dozvoljen");
        }

        ticket.setStatus(cilj);

        // Vremenske oznake se postavljaju samo pri prvom ulasku u stanje -
        // nose V6 statistiku (koliko se cekalo, koliko je trajala obrada).
        if (cilj == TicketStatus.CALLED && ticket.getCalledAt() == null) {
            ticket.setCalledAt(OffsetDateTime.now());
        }
        if (cilj == TicketStatus.COMPLETED) {
            ticket.setCompletedAt(OffsetDateTime.now());
        }

        // Bez save(): ticket je ucitan u ovoj transakciji, dirty checking
        // sam posalje UPDATE na kraju.
        events.objavi(TicketEvent.promijenjen(ticket));
        cache.evictAfterCommit(ticket.getQueue().getId());

        return TicketResponse.from(ticket);
    }
}
