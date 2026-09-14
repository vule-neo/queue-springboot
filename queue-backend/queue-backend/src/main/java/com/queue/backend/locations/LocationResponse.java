package com.queue.backend.locations;

import java.time.OffsetDateTime;

public record LocationResponse(
        Long id,
        Long organizationId,
        String name,
        String address,
        OffsetDateTime createdAt) {

    public static LocationResponse from(Location l) {
        // getOrganization().getId() NE ide u bazu - id proxy vec zna iz FK kolone.
        return new LocationResponse(
                l.getId(),
                l.getOrganization().getId(),
                l.getName(),
                l.getAddress(),
                l.getCreatedAt());
    }
}
