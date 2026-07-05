package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String entityType,
        UUID entityId,
        UUID actorUserId,
        String actorName,
        String actorEmail,
        Instant createdAt
) {
}
