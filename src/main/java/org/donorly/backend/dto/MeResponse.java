package org.donorly.backend.dto;

import java.util.List;
import java.util.UUID;

/** Current authenticated user + org branding (refreshed via GET /api/auth/me). */
public record MeResponse(
        UUID userId,
        String fullName,
        boolean platformAdmin,
        UUID organizationId,
        String organizationName,
        String organizationPrimaryColor,
        /** Logo URL path or absolute URL — never inline base64. */
        String organizationLogo,
        String roleCode,
        List<String> permissions
) {}
