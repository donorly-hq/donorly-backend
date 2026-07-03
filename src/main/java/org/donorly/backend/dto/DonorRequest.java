package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record DonorRequest(
        @NotBlank String fullName,
        String email,
        String phone,
        String city,
        String donorType,
        String status
) {
}
