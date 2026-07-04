package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

/** Lightweight org row for platform list views — no logo payload. */
public record OrganizationSummary(
        UUID id,
        String name,
        String slug,
        String vertical,
        String status,
        String timezone,
        String primaryColor,
        boolean hasLogo,
        Instant createdAt,
        UUID ownerId,
        String ownerName,
        String ownerEmail
) {}
