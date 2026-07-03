package org.donorly.backend.dto;

import java.math.BigDecimal;

public record PledgeUpdateRequest(
        BigDecimal amount,
        BigDecimal collectedAmount,
        String status,
        String paymentMethod,
        String notes
) {
}
