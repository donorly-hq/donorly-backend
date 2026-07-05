package org.donorly.backend.dto;

import java.util.List;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String name,
        String category,
        int quantity,
        String notes,
        int unitsOut,
        int unitsOverdue,
        List<InventoryUnitStatus> units
) {}
