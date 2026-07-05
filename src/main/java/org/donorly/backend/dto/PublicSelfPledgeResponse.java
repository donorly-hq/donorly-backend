package org.donorly.backend.dto;

import java.math.BigDecimal;

public record PublicSelfPledgeResponse(
        String donorName,
        BigDecimal amount,
        String campaignName,
        String organizationName
) {}
