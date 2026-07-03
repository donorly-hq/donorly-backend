package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID pledgeId,
        UUID donorId,
        String donorName,
        BigDecimal amount,
        String paymentMethod,
        LocalDate paymentDate,
        String reference,
        String notes,
        UUID recordedBy,
        Instant createdAt,
        ReceiptResponse receipt
) {
}
