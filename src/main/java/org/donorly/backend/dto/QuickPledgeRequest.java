package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** One-tap pledge entry at live events: donor is found or created by name/contact. */
public record QuickPledgeRequest(
        @NotNull UUID campaignId,
        @NotBlank String donorName,
        String phone,
        String email,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod
) {
}
