package com.queue.backend.organizations;

import java.time.OffsetDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String slug,
        OffsetDateTime createdAt) {

    // Mapiranje na jednom mjestu umjesto prepisivanja po servisu.
    public static OrganizationResponse from(Organization o) {
        return new OrganizationResponse(o.getId(), o.getName(), o.getSlug(), o.getCreatedAt());
    }
}
