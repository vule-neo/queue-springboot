package com.queue.backend.organizations;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.NotFoundException;

@Service
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setSlug(request.slug());
        // Duplikat slug-a ne provjeravamo rucno - to hvata uq_organization_slug
        // u bazi. Rucna provjera pa upis nije atomicna.
        return OrganizationResponse.from(repository.save(organization));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> findAll() {
        return repository.findAll().stream().map(OrganizationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id) {
        return repository.findById(id)
                .map(OrganizationResponse::from)
                .orElseThrow(() -> new NotFoundException("Organizacija " + id + " ne postoji"));
    }

    /** Sam entitet, za druge servise. Ne smije zavrsiti u kontroleru. */
    @Transactional(readOnly = true)
    public Organization getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organizacija " + id + " ne postoji"));
    }
}
