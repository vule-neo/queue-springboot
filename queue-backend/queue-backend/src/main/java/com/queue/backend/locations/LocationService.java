package com.queue.backend.locations;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.organizations.Organization;
import com.queue.backend.organizations.OrganizationService;

@Service
public class LocationService {

    private final LocationRepository repository;
    private final OrganizationService organizationService;

    public LocationService(LocationRepository repository, OrganizationService organizationService) {
        this.repository = repository;
        this.organizationService = organizationService;
    }

    @Transactional
    public LocationResponse create(CreateLocationRequest request) {
        // Roditelj ide kroz tudji SERVIS, ne kroz tudji repozitorij -
        // pravilo "nema organizacije -> 404" tako zivi na jednom mjestu.
        Organization organization = organizationService.getEntity(request.organizationId());

        Location location = new Location();
        location.setOrganization(organization);
        location.setName(request.name());
        location.setAddress(request.address());

        return LocationResponse.from(repository.save(location));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findAll() {
        return repository.findAll().stream().map(LocationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse findById(Long id) {
        return repository.findById(id)
                .map(LocationResponse::from)
                .orElseThrow(() -> new NotFoundException("Lokacija " + id + " ne postoji"));
    }

    /** Sam entitet, za druge servise. Ne smije zavrsiti u kontroleru. */
    @Transactional(readOnly = true)
    public Location getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lokacija " + id + " ne postoji"));
    }
}
