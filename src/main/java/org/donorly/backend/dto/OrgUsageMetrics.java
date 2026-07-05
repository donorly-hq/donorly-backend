package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Per-tenant usage snapshot for the platform admin console. */
public record OrgUsageMetrics(
        UUID organizationId,
        long activeMembers,
        long donorCount,
        long activeCampaigns,
        long pledgeCount,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        Instant lastActivityAt
) {
}
