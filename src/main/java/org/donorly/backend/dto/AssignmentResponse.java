package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID donorId,
        String donorName,
        UUID ambassadorUserId,
        String ambassadorName,
        UUID campaignId,
        String status,
        Instant createdAt
) {
}
