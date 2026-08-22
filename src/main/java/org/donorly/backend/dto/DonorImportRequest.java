package org.donorly.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DonorImportRequest(
        @NotEmpty @Size(max = 1000) List<Row> donors,
        String defaultBucket,   // bucket applied to rows without their own (bulk assignment)
        String groupName        // group/tag applied to every imported donor, e.g. "Pilot 1200"
) {
    public record Row(
            String fullName,
            String email,
            String phone,
            String city,
            String state,
            String address,
            String donorType,
            String bucket
    ) {
    }
}
