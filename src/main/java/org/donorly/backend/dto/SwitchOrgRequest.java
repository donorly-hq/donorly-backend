package org.donorly.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwitchOrgRequest(@NotNull UUID organizationId) {}
