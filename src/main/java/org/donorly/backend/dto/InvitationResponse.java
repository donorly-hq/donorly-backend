package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String roleCode,
        String roleName,
        String status,
        Instant expiresAt,
        Instant createdAt,
        String inviteToken
) {
}
