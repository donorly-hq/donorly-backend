package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
        UUID membershipId,
        UUID userId,
        String fullName,
        String email,
        String roleCode,
        String roleName,
        String status,
        Instant lastLoginAt
) {
}
