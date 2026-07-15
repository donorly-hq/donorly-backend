package org.donorly.backend.dto;

import org.donorly.backend.model.Pledge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API shape for a pledge — decouples the wire format from the JPA entity. */
public record PledgeResponse(
        UUID id,
        UUID organizationId,
        UUID campaignId,
        UUID donorId,
        BigDecimal amount,
        BigDecimal collectedAmount,
        String frequency,
        String paymentMethod,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String source,
        String notes,
        Instant lastReminderAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PledgeResponse from(Pledge p) {
        return new PledgeResponse(p.getId(), p.getOrganizationId(), p.getCampaignId(), p.getDonorId(),
                p.getAmount(), p.getCollectedAmount(), p.getFrequency(), p.getPaymentMethod(),
                p.getStartDate(), p.getEndDate(), p.getStatus(), p.getSource(), p.getNotes(),
                p.getLastReminderAt(), p.getCreatedAt(), p.getModifiedAt());
    }
}
