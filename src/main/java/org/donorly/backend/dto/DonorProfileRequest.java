package org.donorly.backend.dto;

public record DonorProfileRequest(
        String occupation,
        String employer,
        String preferredLanguage,
        String preferredChannel,
        String notesPrivate
) {
}
