package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fields the AI read off a pledge card photo, enriched with server-side matches
 * (campaign resolved by name, donor matched against existing records).
 * Everything here is a *suggestion* — the user verifies and edits before saving.
 */
public record PledgeCardScanResponse(
        String donorFullName,
        String donorEmail,
        String donorPhone,
        String donorCity,
        String donorType,          // "individual" | "organization"
        BigDecimal amount,
        String paymentMethod,      // "cash" | "check" | "card" | null
        UUID campaignId,           // resolved when campaign name matched an org campaign
        String campaignName,
        UUID matchedDonorId,       // existing donor this likely belongs to, if any
        String matchedDonorName,
        String notes,
        String extractedJson       // raw model output, stored for the audit trail
) {
}
