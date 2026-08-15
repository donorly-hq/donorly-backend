package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A pledge the reminder engine would nag about, surfaced for human review.
 * Carries the exact email that would go out so the user approves what they see
 * (generate → review → send).
 */
public record SuggestedReminderResponse(
        UUID pledgeId,
        UUID donorId,
        String donorName,
        String donorEmail,
        String campaignName,
        BigDecimal amount,
        BigDecimal collected,
        BigDecimal outstanding,
        Instant lastReminderAt,
        Instant pledgedAt,
        String emailSubject,
        String emailBody
) {
}
