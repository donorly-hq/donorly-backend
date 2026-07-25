package org.donorly.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetOwnerRequest(

        @NotBlank
        @Size(max = 200)
        String ownerName,

        @NotBlank
        @Email
        @Size(max = 200)
        String ownerEmail
) {}
