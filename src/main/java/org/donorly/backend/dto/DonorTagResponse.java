package org.donorly.backend.dto;

import java.util.UUID;

public record DonorTagResponse(
        UUID id,
        String name,
        String color
) {
}
