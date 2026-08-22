package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DonorRequest(
        @NotBlank String fullName,
        String email,
        String phone,
        String city,
        String state,
        String address,
        String donorType,
        String status,
        UUID assignedToUserId,       // point of contact
        Boolean majorDonor,
        String bucket,               // confirmed | potential | re_registering
        String complianceStatus      // ok | non_compliant | claims_paid | non_responsive
) {
}
