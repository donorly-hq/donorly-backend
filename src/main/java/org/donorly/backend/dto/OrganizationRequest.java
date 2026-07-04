package org.donorly.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug may only contain lowercase letters, digits and hyphens")
        String slug,

        String vertical,

        String timezone,

        String logoUrl,

        String logoData,

        String primaryColor,

        // ── Owner account (optional on create, ignored on update) ────────────
        @Size(max = 200)
        String ownerName,

        @Email
        @Size(max = 200)
        String ownerEmail,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String ownerPassword
) {}

