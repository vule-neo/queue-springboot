package com.queue.backend.servicetypes;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/services")
public class ServiceTypeController {

    private final ServiceTypeService service;

    public ServiceTypeController(ServiceTypeService service) {
        this.service = service;
    }

    @PostMapping
    // Namjestanje sistema radi samo menadzer. Anotacija je na KONTROLERU,
    // ne na servisu: u V5 ce RabbitListener zvati iste servise bez
    // SecurityContext-a, pa bi ga @PreAuthorize na servisu odbio.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceTypeResponse create(@Valid @RequestBody CreateServiceTypeRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ServiceTypeResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ServiceTypeResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
