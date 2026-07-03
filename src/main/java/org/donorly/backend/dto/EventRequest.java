package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record EventRequest(
        @NotBlank String name,
        String description,
        String eventType,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        String status,
        UUID campaignId
) {
}
