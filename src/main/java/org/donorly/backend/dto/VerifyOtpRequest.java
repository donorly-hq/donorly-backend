package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String challengeId,
        @NotBlank String code
) {
}
