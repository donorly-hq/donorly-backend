package org.donorly.backend.dto;

import org.donorly.backend.model.Campaign;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API shape for a campaign — decouples the wire format from the JPA entity. */
public record CampaignResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        String campaignType,
        BigDecimal goalAmount,
        LocalDate startDate,
        LocalDate endDate,
        UUID managedByUserId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CampaignResponse from(Campaign c) {
        return new CampaignResponse(c.getId(), c.getOrganizationId(), c.getName(), c.getSlug(),
                c.getCampaignType(), c.getGoalAmount(), c.getStartDate(), c.getEndDate(),
                c.getManagedByUserId(), c.getStatus(), c.getCreatedAt(), c.getModifiedAt());
    }
}
