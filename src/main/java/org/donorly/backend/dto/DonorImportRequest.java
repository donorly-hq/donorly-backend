package org.donorly.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DonorImportRequest(
        @NotEmpty @Size(max = 1000) List<Row> donors
) {
    public record Row(
            String fullName,
            String email,
            String phone,
            String city,
            String donorType
    ) {
    }
}
