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
        /** Opaque id to pass back to /auth/verify-otp or /auth/select-org. */
        String challengeId,
        /** True when the user belongs to several orgs and must pick one to finish login. */
        boolean orgSelectionRequired,
        /** The organizations to choose from (org selection only). */
        List<OrgChoice> organizations
) {

    public static LoginResponse otpChallenge(String challengeId) {
        return new LoginResponse(null, null, null, false, null, null, null, null, null, null,
                true, challengeId, false, null);
    }

    public static LoginResponse orgSelection(String challengeId, List<OrgChoice> organizations) {
        return new LoginResponse(null, null, null, false, null, null, null, null, null, null,
                false, challengeId, true, organizations);
    }
}
