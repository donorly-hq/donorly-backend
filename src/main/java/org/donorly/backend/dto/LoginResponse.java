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
        /** Resolved logo URL path or absolute URL — never inline base64. */
        String organizationLogo,
        String roleCode,
        List<String> permissions,
        /** True when a one-time code was emailed and must be verified to finish login. */
        boolean otpRequired,
        /** Opaque id to pass back to /auth/verify-otp together with the emailed code. */
        String challengeId
) {

    public static LoginResponse otpChallenge(String challengeId) {
        return new LoginResponse(null, null, null, false, null, null, null, null, null, null,
                true, challengeId);
    }
}
