package com.queue.backend.organizations;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    // Namjestanje sistema radi samo menadzer. Anotacija je na KONTROLERU,
    // ne na servisu: u V5 ce RabbitListener zvati iste servise bez
    // SecurityContext-a, pa bi ga @PreAuthorize na servisu odbio.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<OrganizationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OrganizationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
