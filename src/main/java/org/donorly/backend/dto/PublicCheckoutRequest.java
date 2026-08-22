package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PublicCheckoutRequest(
        @NotNull @Positive BigDecimal amount,
        String donorName,
        String donorEmail
) {
}
