package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record FollowUpRequest(
        @NotNull UUID donorId,
        UUID campaignId,
        UUID assignedToUserId,
        Instant dueAt,
        String notes
) {
}
