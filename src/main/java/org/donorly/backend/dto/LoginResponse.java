package org.donorly.backend.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        String fullName,
        boolean platformAdmin,
        UUID organizationId,
        String organizationName,
        String roleCode,
        List<String> permissions
) {
}
