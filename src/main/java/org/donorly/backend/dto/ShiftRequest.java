package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record ShiftRequest(
        @NotBlank String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        Integer slots,
        String status
) {
}
