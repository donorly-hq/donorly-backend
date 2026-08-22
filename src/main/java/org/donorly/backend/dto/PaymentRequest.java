package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Either {@code pledgeId} (payment against a pledge) or {@code campaignId} +
 * {@code donorId} (direct "takaza" donation with no pledge) must be provided.
 */
public record PaymentRequest(
        UUID pledgeId,
        UUID campaignId,
        UUID donorId,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod,
        LocalDate paymentDate,
        String reference,
        String notes,
        // Boxed so clients may omit the field (Jackson 3 rejects null -> primitive).
        Boolean issueReceipt
) {
    public boolean shouldIssueReceipt() {
        return Boolean.TRUE.equals(issueReceipt);
    }
}
