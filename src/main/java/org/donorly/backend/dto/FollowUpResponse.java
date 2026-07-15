package org.donorly.backend.dto;

import org.donorly.backend.model.FollowUp;

import java.time.Instant;
import java.util.UUID;

/** API shape for a follow-up — decouples the wire format from the JPA entity. */
public record FollowUpResponse(
        UUID id,
        UUID organizationId,
        UUID donorId,
        UUID campaignId,
        UUID assignedToUserId,
        Instant dueAt,
        String status,
        String outcome,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static FollowUpResponse from(FollowUp f) {
        return new FollowUpResponse(f.getId(), f.getOrganizationId(), f.getDonorId(), f.getCampaignId(),
                f.getAssignedToUserId(), f.getDueAt(), f.getStatus(), f.getOutcome(), f.getNotes(),
                f.getCreatedAt(), f.getModifiedAt());
    }
}
