package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record DonorNoteResponse(
        UUID id,
        UUID donorId,
        String noteText,
        String noteType,
        String visibility,
        UUID createdBy,
        String createdByName,
        Instant createdAt
) {
}
