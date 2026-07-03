package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CampaignRequest(
        @NotBlank String name,
        String slug,
        String campaignType,
        @PositiveOrZero BigDecimal goalAmount,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        UUID managedByUserId
) {
}
