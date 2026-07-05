package org.donorly.backend.dto;

import org.donorly.backend.model.Donor;

import java.util.List;

/** A cluster of donors that look like the same person. */
public record DuplicateGroupResponse(
        String reason,
        List<Donor> donors
) {
}
