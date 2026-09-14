package com.queue.backend.servicetypes;

import java.time.OffsetDateTime;

public record ServiceTypeResponse(
        Long id,
        Long organizationId,
        String name,
        int avgDurationMinutes,
        OffsetDateTime createdAt) {

    public static ServiceTypeResponse from(ServiceType s) {
        return new ServiceTypeResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getName(),
                s.getAvgDurationMinutes(),
                s.getCreatedAt());
    }
}
