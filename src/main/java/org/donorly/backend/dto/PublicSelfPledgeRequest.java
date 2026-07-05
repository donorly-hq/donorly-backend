package org.donorly.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Unauthenticated pledge from a donor's own phone (Event Mode QR). */
public record PublicSelfPledgeRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 50) String phone,
        @Size(max = 255) String email,
        @NotNull @DecimalMin("1") BigDecimal amount
) {}
