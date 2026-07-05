package org.donorly.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryItemRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String category,
        @Min(1) @Max(10000) int quantity,
        @Size(max = 2000) String notes
) {}
