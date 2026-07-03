package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RegistrationRequest(
        @NotBlank String guestName,
        String guestEmail,
        String guestPhone,
        Integer partySize,
        UUID donorId,
        String notes
) {
}
