package org.donorly.backend.dto;

import java.util.UUID;

public record EventSummaryResponse(
        UUID eventId,
        String name,
        String status,
        Integer capacity,
        long registrationCount,
        long checkedInCount,
        long totalGuests,
        long shiftCount,
        long volunteerSlots,
        long volunteerFilled
) {
}
