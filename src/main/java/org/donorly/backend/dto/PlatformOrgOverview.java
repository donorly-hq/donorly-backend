package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One tile on the platform admin dashboard: identity, onboarding progress and
 * fundraising thermometer totals for a single tenant.
 */
public record PlatformOrgOverview(
        UUID id,
        String name,
        String slug,
        String vertical,
        String status,
        String primaryColor,
        boolean hasLogo,
        int setupPercent,
        long activeMembers,
        long donorCount,
        long activeCampaigns,
        BigDecimal goalTotal,
        BigDecimal pledgedTotal,
        BigDecimal collectedTotal
) {
}
