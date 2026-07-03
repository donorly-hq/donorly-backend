package org.donorly.backend.dto;

public record SendResultResponse(
        int sent,
        int skipped,
        int failed
) {
}
