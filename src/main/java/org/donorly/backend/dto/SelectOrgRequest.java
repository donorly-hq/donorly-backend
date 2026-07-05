package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectOrgRequest(
        @NotBlank String challengeId,
        @NotNull UUID organizationId
) {}
