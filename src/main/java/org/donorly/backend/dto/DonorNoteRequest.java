package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record DonorNoteRequest(
        @NotBlank String noteText,
        String noteType,
        String visibility
) {
}
