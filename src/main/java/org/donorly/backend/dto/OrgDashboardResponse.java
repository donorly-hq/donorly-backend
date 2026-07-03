package org.donorly.backend.dto;

import java.math.BigDecimal;

public record OrgDashboardResponse(
        long totalDonors,
        long totalCampaigns,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        BigDecimal remaining,
        long openFollowUps
) {
}
