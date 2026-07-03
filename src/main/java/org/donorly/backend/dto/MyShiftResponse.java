package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record MyShiftResponse(
        UUID assignmentId,
        String status,
        Instant checkedInAt,
        UUID shiftId,
        String shiftTitle,
        Instant shiftStartsAt,
        UUID eventId,
        String eventName,
        String eventLocation
) {
}
