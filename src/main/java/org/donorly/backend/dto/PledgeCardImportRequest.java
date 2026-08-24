package org.donorly.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Bulk pledge-card import from a spreadsheet (CSV/Excel parsed client-side).
 * Batch-level defaults apply to every row that doesn't carry its own value.
 */
public record PledgeCardImportRequest(
        @NotEmpty @Size(max = 1000) List<Row> cards,
        UUID defaultCampaignId,     // used when a row has no matching campaign name
        String batch,               // batch label stamped on every imported card
        UUID pointOfContactUserId   // POC for every card; defaults to the importer
) {
    public record Row(
            String donorFullName,
            String donorEmail,
            String donorPhone,
            String donorCity,
            String donorType,
            BigDecimal amount,
            String paymentMethod,
            String campaignName,    // fuzzy-matched against the org's campaigns
            String notes
    ) {
    }
}
