package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PledgeCardResponse(
        UUID id,
        UUID campaignId,
        String campaignName,
        UUID donorId,
        String donorName,
        String imageUrl,
        BigDecimal amount,
        String paymentMethod,
        String notes,
        String verificationStatus,
        UUID createdBy,
        Instant createdAt
) {
}
