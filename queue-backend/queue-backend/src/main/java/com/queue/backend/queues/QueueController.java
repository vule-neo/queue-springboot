package com.queue.backend.queues;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

    private final QueueService service;

    public QueueController(QueueService service) {
        this.service = service;
    }

    @PostMapping
    // Namjestanje sistema radi samo menadzer. Anotacija je na KONTROLERU,
    // ne na servisu: u V5 ce RabbitListener zvati iste servise bez
    // SecurityContext-a, pa bi ga @PreAuthorize na servisu odbio.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueResponse create(@Valid @RequestBody CreateQueueRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<QueueResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public QueueResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
