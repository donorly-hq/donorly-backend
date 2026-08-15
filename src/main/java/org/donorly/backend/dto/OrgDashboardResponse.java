package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Everything the org command-center dashboard needs in one round trip:
 * KPI totals, per-campaign thermometer data, recent donations and the
 * follow-ups that need attention first.
 */
public record OrgDashboardResponse(
        long totalDonors,
        long totalCampaigns,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        BigDecimal remaining,
        long openFollowUps,
        long outstandingPledges,
        List<CampaignProgress> campaigns,
        List<RecentPayment> recentPayments,
        List<DueFollowUp> dueFollowUps
) {
    /** Active campaign with goal vs pledged/collected — powers a thermometer card. */
    public record CampaignProgress(
            UUID id,
            String name,
            String status,
            BigDecimal goalAmount,
            BigDecimal pledged,
            BigDecimal collected,
            LocalDate endDate
    ) {
    }

    public record RecentPayment(
            UUID id,
            String donorName,
            BigDecimal amount,
            String paymentMethod,
            LocalDate paymentDate
    ) {
    }

    public record DueFollowUp(
            UUID id,
            UUID donorId,
            String donorName,
            Instant dueAt,
            String notes
    ) {
    }
}
