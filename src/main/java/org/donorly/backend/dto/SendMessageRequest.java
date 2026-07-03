package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID donorId,
        @NotBlank String channel,
        UUID templateId,
        String subject,
        String body
) {
}
