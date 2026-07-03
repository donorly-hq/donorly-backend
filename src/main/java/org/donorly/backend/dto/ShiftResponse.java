package org.donorly.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID eventId,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int slots,
        int filled,
        String status,
        List<VolunteerAssignmentResponse> assignments
) {
}
