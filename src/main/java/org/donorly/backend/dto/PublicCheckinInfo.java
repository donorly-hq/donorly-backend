package org.donorly.backend.dto;

import java.time.Instant;

/** Registration details shown on the self-check-in confirmation screen. */
public record PublicCheckinInfo(
        String eventName,
        String eventLocation,
        Instant eventStartsAt,
        String guestName,
        int partySize,
        String status,
        Instant checkedInAt
) {
}
