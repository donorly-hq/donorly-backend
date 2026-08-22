package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Optional server-side filters for the donors list. Null fields are ignored. */
public record DonorFilter(
        UUID tagId,
        String state,
        String bucket,
        String complianceStatus,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        boolean majorOnly
) {
    public static final DonorFilter NONE = new DonorFilter(null, null, null, null, null, null, false);

    public boolean isEmpty() {
        return tagId == null && state == null && bucket == null && complianceStatus == null
                && minAmount == null && maxAmount == null && !majorOnly;
    }
}
