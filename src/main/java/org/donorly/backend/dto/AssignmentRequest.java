package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignmentRequest(
        @NotNull UUID ambassadorUserId,
        UUID campaignId
) {
}
