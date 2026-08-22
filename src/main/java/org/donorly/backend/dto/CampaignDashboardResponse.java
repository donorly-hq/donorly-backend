package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CampaignDashboardResponse(
        UUID campaignId,
        String name,
        BigDecimal goalAmount,
        BigDecimal pledged,
        BigDecimal collected,
        BigDecimal remaining,
        int pledgeCount,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Integer daysRemaining,   // null when no end date
        int donorsTargeted
) {
}
