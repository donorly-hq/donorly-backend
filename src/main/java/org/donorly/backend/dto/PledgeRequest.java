package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PledgeRequest(
        @NotNull UUID donorId,
        @NotNull @Positive BigDecimal amount,
        String frequency,
        String paymentMethod,
        LocalDate startDate,
        LocalDate endDate,
        String source,
        String notes
) {
}
