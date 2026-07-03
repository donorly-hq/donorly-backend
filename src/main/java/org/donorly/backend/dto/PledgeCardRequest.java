package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PledgeCardRequest(

        /* ── optional campaign link ────────────────────────────── */
        UUID campaignId,

        /* ── donor: either supply an existing id OR inline fields ─ */
        UUID donorId,

        @NotBlank String donorFullName,
        String donorEmail,
        String donorPhone,
        String donorCity,
        String donorType,          // "individual" | "organization"

        /* ── pledge card body ──────────────────────────────────── */
        String imageUrl,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod,
        String notes
) {}
