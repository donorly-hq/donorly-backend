package org.donorly.backend.dto;

import java.time.Instant;

public record FollowUpUpdateRequest(
        String status,
        String outcome,
        String notes,
        Instant dueAt
) {
}
