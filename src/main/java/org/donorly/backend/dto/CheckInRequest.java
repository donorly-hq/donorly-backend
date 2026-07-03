package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(
        @NotBlank String code
) {
}
