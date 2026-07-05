package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Anonymized live tally, safe to display on a public screen. */
public record PublicThermometerResponse(
        String organizationName,
        String campaignName,
        BigDecimal goalAmount,
        BigDecimal pledged,
        BigDecimal collected,
        int pledgeCount,
        List<RecentPledge> recentPledges
) {
    /** Donor names are abbreviated to first name + last initial. */
    public record RecentPledge(
            String donorName,
            BigDecimal amount,
            Instant createdAt
    ) {
    }
}
