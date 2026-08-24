package org.donorly.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** A donor's answer from the reminder email's response page. */
public record DonorRespondRequest(
        @NotBlank String action,    // paid | promise | stop
        LocalDate promiseDate,      // required when action = promise
        String comment              // optional free text, AI-classified
) {
}
