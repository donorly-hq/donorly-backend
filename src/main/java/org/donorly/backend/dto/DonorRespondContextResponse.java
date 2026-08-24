package org.donorly.backend.dto;

import java.math.BigDecimal;

/** What the public response page needs to greet the donor. */
public record DonorRespondContextResponse(
        String orgName,
        String donorFirstName,
        BigDecimal amount,
        String campaignName,
        boolean used,
        boolean expired
) {
}
