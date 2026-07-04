package org.donorly.backend.dto;

import java.util.UUID;

/** Active org member — used by platform admins when transferring ownership. */
public record OrgMemberSummary(
        UUID userId,
        String fullName,
        String email,
        String roleCode,
        String roleName,
        String status
) {}
