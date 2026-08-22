package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Optional server-side filters for the pledge-cards list (all nullable = no filter).
 * Location and compliance are resolved through the linked donor.
 */
public record PledgeCardFilter(
        String status,
        String batch,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Instant enteredAfter,
        Instant enteredBefore,
        String location,
        String compliance
) {
}
