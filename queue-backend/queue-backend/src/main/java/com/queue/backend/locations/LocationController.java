package com.queue.backend.locations;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @PostMapping
    // Namjestanje sistema radi samo menadzer. Anotacija je na KONTROLERU,
    // ne na servisu: u V5 ce RabbitListener zvati iste servise bez
    // SecurityContext-a, pa bi ga @PreAuthorize na servisu odbio.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@Valid @RequestBody CreateLocationRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<LocationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LocationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
