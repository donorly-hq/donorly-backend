package org.donorly.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Check a unit out. Holder is either a team member (holderUserId) or a free-text name. */
public record InventoryAssignRequest(
        @Min(1) int unitNumber,
        UUID holderUserId,
        @Size(max = 200) String holderName,
        LocalDate expectedReturnDate,
        @Size(max = 2000) String notes
) {}
