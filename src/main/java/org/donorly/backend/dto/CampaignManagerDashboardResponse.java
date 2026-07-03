package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CampaignManagerDashboardResponse(

        /* ── managed campaigns ─────────────────────────────── */
        List<ManagedCampaign> managedCampaigns,
        long totalManagedCampaigns,

        /* ── ambassadors I created ─────────────────────────── */
        List<AmbassadorItem> myAmbassadors,

        /* ── aggregate stats across my campaigns ───────────── */
        long totalDonors,
        long openFollowUps,
        long pledgeCount,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        BigDecimal outstanding

) {
    public record ManagedCampaign(
            UUID id, String name, String campaignType, String status,
            BigDecimal goalAmount, BigDecimal pledged, BigDecimal collected,
            LocalDate startDate, LocalDate endDate
    ) {}

    public record AmbassadorItem(
            UUID userId, String fullName, String email, String memberStatus
    ) {}
}
