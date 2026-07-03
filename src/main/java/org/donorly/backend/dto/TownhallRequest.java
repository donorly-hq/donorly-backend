package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TownhallRequest(
        @NotBlank String personName,
        String phone,
        String venue,
        String address,
        LocalDate eventDate,
        LocalTime eventTime,
        Integer durationMinutes,
        UUID hostAmbassadorUserId,
        Integer expectedRsvps,
        String notes
) {}
