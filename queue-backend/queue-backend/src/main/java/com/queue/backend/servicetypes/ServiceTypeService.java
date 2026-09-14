package com.queue.backend.servicetypes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.organizations.Organization;
import com.queue.backend.organizations.OrganizationService;

@Service
public class ServiceTypeService {

    private final ServiceTypeRepository repository;
    private final OrganizationService organizationService;

    public ServiceTypeService(ServiceTypeRepository repository, OrganizationService organizationService) {
        this.repository = repository;
        this.organizationService = organizationService;
    }

    @Transactional
    public ServiceTypeResponse create(CreateServiceTypeRequest request) {
        Organization organization = organizationService.getEntity(request.organizationId());

        ServiceType serviceType = new ServiceType();
        serviceType.setOrganization(organization);
        serviceType.setName(request.name());
        serviceType.setAvgDurationMinutes(request.avgDurationMinutes());

        return ServiceTypeResponse.from(repository.save(serviceType));
    }

    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> findAll() {
        return repository.findAll().stream().map(ServiceTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceTypeResponse findById(Long id) {
        return repository.findById(id)
                .map(ServiceTypeResponse::from)
                .orElseThrow(() -> new NotFoundException("Usluga " + id + " ne postoji"));
    }

    @Transactional(readOnly = true)
    public ServiceType getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usluga " + id + " ne postoji"));
    }
}
