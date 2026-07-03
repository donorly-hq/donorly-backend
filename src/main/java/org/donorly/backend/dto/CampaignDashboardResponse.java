package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CampaignDashboardResponse(
        UUID campaignId,
        String name,
        BigDecimal goalAmount,
        BigDecimal pledged,
        BigDecimal collected,
        BigDecimal remaining,
        int pledgeCount
) {
}
