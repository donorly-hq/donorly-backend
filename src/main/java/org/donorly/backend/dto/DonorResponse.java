package org.donorly.backend.dto;

import org.donorly.backend.model.Donor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** API shape for a donor — decouples the wire format from the JPA entity. */
public record DonorResponse(
        UUID id,
        UUID organizationId,
        String fullName,
        String email,
        String phone,
        String city,
        String donorType,
        String status,
        BigDecimal lifetimeGiving,
        Instant createdAt,
        Instant updatedAt
) {
    public static DonorResponse from(Donor d) {
        return new DonorResponse(d.getId(), d.getOrganizationId(), d.getFullName(), d.getEmail(),
                d.getPhone(), d.getCity(), d.getDonorType(), d.getStatus(), d.getLifetimeGiving(),
                d.getCreatedAt(), d.getModifiedAt());
    }
}
