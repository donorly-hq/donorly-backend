package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String vertical,
        String status,
        String timezone,
        String logoUrl,
        String primaryColor,
        Instant createdAt,
        // owner summary — null when org was bootstrapped without an explicit owner
        UUID ownerId,
        String ownerName,
        String ownerEmail
) {}
