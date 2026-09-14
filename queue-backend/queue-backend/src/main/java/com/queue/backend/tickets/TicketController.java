package com.queue.backend.tickets;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bez @RequestMapping na klasi: putanje su iz dva korijena
 * (/api/queues i /api/tickets), pa su pune.
 */
@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ---------- musterija ----------

    // Nema @RequestBody: queueId je u putanji, broj racuna server.
    @PostMapping("/api/queues/{queueId}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@PathVariable Long queueId) {
        return ticketService.create(queueId);
    }

    @GetMapping("/api/queues/{queueId}/tickets")
    public List<TicketResponse> findByQueue(@PathVariable Long queueId) {
        return ticketService.findByQueue(queueId);
    }

    @GetMapping("/api/tickets/{id}")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    // Otkazivanje smije svaki prijavljeni.
    // TODO: ticket jos ne zna ciji je, pa se ne moze provjeriti da otkazujes
    // SVOJ ticket. Vidi napomenu o ticket.user_id.
    @PostMapping("/api/tickets/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id) {
        return ticketService.cancel(id);
    }

    // ---------- salter ----------

    @PostMapping("/api/queues/{queueId}/next")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse callNext(@PathVariable Long queueId) {
        return ticketService.callNext(queueId);
    }

    @PostMapping("/api/tickets/{id}/call")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse call(@PathVariable Long id) {
        return ticketService.call(id);
    }

    @PostMapping("/api/tickets/{id}/serve")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse startServing(@PathVariable Long id) {
        return ticketService.startServing(id);
    }

    @PostMapping("/api/tickets/{id}/complete")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse complete(@PathVariable Long id) {
        return ticketService.complete(id);
    }

    @PostMapping("/api/tickets/{id}/no-show")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse noShow(@PathVariable Long id) {
        return ticketService.noShow(id);
    }

    @PostMapping("/api/tickets/{id}/skip")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse skip(@PathVariable Long id) {
        return ticketService.skip(id);
    }

    @PostMapping("/api/tickets/{id}/requeue")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    public TicketResponse requeue(@PathVariable Long id) {
        return ticketService.requeue(id);
    }
}
