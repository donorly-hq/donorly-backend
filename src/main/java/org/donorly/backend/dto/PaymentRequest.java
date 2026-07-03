package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRequest(
        @NotNull UUID pledgeId,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod,
        LocalDate paymentDate,
        String reference,
        String notes,
        boolean issueReceipt
) {
}
