package org.donorly.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Merge {@code mergeIds} into {@code keepId}; merged donors are soft-deleted. */
public record DonorMergeRequest(
        @NotNull UUID keepId,
        @NotEmpty List<UUID> mergeIds
) {
}
