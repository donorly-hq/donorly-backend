package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Snapshot for the projector-friendly live tally screen. */
public record CampaignLiveResponse(
        UUID campaignId,
        String name,
        BigDecimal goalAmount,
        BigDecimal pledged,
        BigDecimal collected,
        int pledgeCount,
        List<RecentPledge> recentPledges
) {
    public record RecentPledge(
            String donorName,
            BigDecimal amount,
            Instant createdAt
    ) {
    }
}
