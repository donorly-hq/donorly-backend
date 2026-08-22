package org.donorly.backend.dto;

import java.util.List;
import java.util.UUID;

/**
 * A campaign's audience: the raw selectors plus the resolved distinct donor
 * count (union of individually attached donors, tag/group members, and donors
 * in targeted states).
 */
public record CampaignAudienceResponse(
        List<Target> targets,
        int targetedDonorCount
) {
    public record Target(
            UUID id,
            UUID donorId,
            String donorName,
            UUID tagId,
            String tagName,
            String state
    ) {
    }
}
