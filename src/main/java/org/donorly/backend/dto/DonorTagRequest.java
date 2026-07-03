package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record DonorTagRequest(
        @NotBlank String name,
        String color
) {
}
