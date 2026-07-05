package org.donorly.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One physical unit of an item. Holder fields are null when the unit is available. */
public record InventoryUnitStatus(
        int unitNumber,
        UUID assignmentId,
        UUID holderUserId,
        String holderName,
        Instant assignedAt,
        LocalDate expectedReturnDate,
        long daysHeld,
        boolean overdue,
        String notes
) {
    public static InventoryUnitStatus available(int unitNumber) {
        return new InventoryUnitStatus(unitNumber, null, null, null, null, null, 0, false, null);
    }
}
