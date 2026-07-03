package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID paymentId,
        String receiptNumber,
        String issuedTo,
        BigDecimal amount,
        Instant issuedAt
) {
}
