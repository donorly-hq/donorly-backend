package org.donorly.backend.dto;

import java.math.BigDecimal;

public record FundraisingReportResponse(
        long totalDonors,
        long activeCampaigns,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        BigDecimal outstanding,
        long totalPledges,
        long fulfilledPledges,
        long openFollowUps,
        long paymentsThisMonth,
        BigDecimal collectedThisMonth
) {
}
