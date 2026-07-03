package org.donorly.backend.dto;

import java.util.UUID;

public record DonorProfileResponse(
        UUID donorId,
        String occupation,
        String employer,
        String preferredLanguage,
        String preferredChannel,
        String notesPrivate
) {
}
