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
        String organizationPrimaryColor,
        /** Resolved logo: uploaded image data (base64 data URL) preferred over logoUrl */
        String organizationLogo,
        String roleCode,
        List<String> permissions
) {
}
