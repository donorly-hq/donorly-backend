package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID pledgeId,
        UUID campaignId,
        String campaignName,
        UUID donorId,
        String donorName,
        BigDecimal amount,
        String paymentMethod,
        LocalDate paymentDate,
        String reference,
        String notes,
        UUID recordedBy,
        Instant createdAt,
        Integer campaignDay,     // "day N ..." — payment date relative to campaign start (1-based)
        Integer campaignDays,    // "... of M" — total campaign length in days
        ReceiptResponse receipt
) {
}
