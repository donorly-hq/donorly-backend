package org.donorly.backend.dto;

public record MemberUpdateRequest(
        String roleCode,
        String status
) {
}
