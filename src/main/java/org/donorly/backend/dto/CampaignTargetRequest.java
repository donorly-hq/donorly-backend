package org.donorly.backend.dto;

import java.util.UUID;

/** Exactly one of the three selectors must be set. */
public record CampaignTargetRequest(
        UUID donorId,
        UUID tagId,
        String state
) {
}
