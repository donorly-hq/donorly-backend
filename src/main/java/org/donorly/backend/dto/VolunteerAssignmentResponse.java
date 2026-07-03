package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record VolunteerAssignmentResponse(
        UUID id,
        UUID shiftId,
        UUID userId,
        String userName,
        String status,
        Instant checkedInAt
) {
}
