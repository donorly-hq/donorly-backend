package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuickPledgeResponse(
        UUID pledgeId,
        UUID donorId,
        String donorName,
        BigDecimal amount,
        boolean newDonor
) {
}
