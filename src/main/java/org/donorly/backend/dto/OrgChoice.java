package org.donorly.backend.dto;

import java.util.UUID;

/** One organization a user belongs to — powers the login picker and topbar switcher. */
public record OrgChoice(
        UUID organizationId,
        String name,
        String slug,
        String logoUrl,
        String primaryColor,
        String roleCode,
        String roleName
) {}
