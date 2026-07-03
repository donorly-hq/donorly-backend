package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BroadcastRequest(
        @NotBlank String channel,
        @NotEmpty List<UUID> donorIds,
        UUID templateId,
        String subject,
        String body
) {
}
