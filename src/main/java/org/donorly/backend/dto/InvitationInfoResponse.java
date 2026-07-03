package org.donorly.backend.dto;

public record InvitationInfoResponse(
        String organizationName,
        String email,
        String roleName,
        boolean valid,
        boolean existingUser
) {
}
