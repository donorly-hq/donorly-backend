package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageTemplateRequest(
        @NotBlank String name,
        @NotBlank String channel,
        String subject,
        @NotBlank String body
) {
}
