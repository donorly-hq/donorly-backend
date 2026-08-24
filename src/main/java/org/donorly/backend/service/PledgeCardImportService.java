package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.PledgeCardImportRequest;
import org.donorly.backend.dto.PledgeCardImportResult;
import org.donorly.backend.dto.PledgeCardRequest;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Bulk pledge-card import from a spreadsheet. Every valid row becomes a card in the
 * normal "pending" queue, so imported cards flow through the same review tabs and
 * auto-approve sweep as scanned or manually entered ones.
 *
 * Unlike {@link PledgeCardService#create}, donors are resolved with
 * {@link DonorMatchingService} first — a 500-row sheet of existing donors must not
 * create 500 duplicate donor records.
 */
@Service
@RequiredArgsConstructor
public class PledgeCardImportService {

    /** Allowed by the donors.donor_type DB check constraint. */
    private static final Set<String> DONOR_TYPES = Set.of("individual", "family", "business", "anonymous");
    private static final Set<String> PAYMENT_METHODS = Set.of("cash", "check", "card");

    private final PledgeCardService pledgeCardService;
    private final DonorMatchingService donorMatchingService;
    private final CampaignMatchingService campaignMatchingService;
    private final CampaignRepository campaignRepository;
    private final AuditService auditService;

    @Transactional
    public PledgeCardImportResult importCards(PledgeCardImportRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();

        UUID defaultCampaignId = request.defaultCampaignId();
        if (defaultCampaignId != null) {
            campaignRepository.findByIdAndOrganizationId(defaultCampaignId, orgId)
                    .orElseThrow(() -> new NotFoundException("Default campaign not found"));
        }
        List<Campaign> campaigns = campaignRepository.findByOrganizationId(orgId);
        String batch = trimToNull(request.batch());

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        // Exact duplicate rows inside the same file (same donor + amount + campaign) are skipped.
        Set<String> seenRows = new HashSet<>();

        int rowNumber = 0;
        for (PledgeCardImportRequest.Row row : request.cards()) {
            rowNumber++;
            String name = trimToNull(row.donorFullName());
            if (name == null) {
                errors.add("Row " + rowNumber + ": donor name is required");
                continue;
            }
            BigDecimal amount = row.amount();
            if (amount == null || amount.signum() <= 0) {
                errors.add("Row " + rowNumber + ": a positive amount is required");
                continue;
            }

            Campaign matched = campaignMatchingService.match(campaigns, row.campaignName());
            UUID campaignId = matched != null ? matched.getId() : defaultCampaignId;

            String rowKey = DonorImportService.normalizeName(name)
                    + "|" + amount.stripTrailingZeros().toPlainString()
                    + "|" + campaignId;
            if (!seenRows.add(rowKey)) {
                skipped++;
                continue;
            }

            String email = trimToNull(row.donorEmail());
            String phone = trimToNull(row.donorPhone());
            // Matching sees donors created earlier in this batch too (same persistence context).
            Donor existing = donorMatchingService.findExistingDonor(orgId, name, email, phone);

            pledgeCardService.create(new PledgeCardRequest(
                    campaignId,
                    existing != null ? existing.getId() : null,
                    name,
                    email,
                    phone,
                    trimToNull(row.donorCity()),
                    normalizeDonorType(row.donorType()),
                    null,   // imageUrl
                    null,   // extractedJson
                    amount,
                    normalizePaymentMethod(row.paymentMethod()),
                    trimToNull(row.notes()),
                    request.pointOfContactUserId(),
                    batch
            ));
            imported++;
        }

        auditService.record("pledge_card.import", "pledge_card", null);
        return new PledgeCardImportResult(imported, skipped, errors);
    }

    private static String normalizeDonorType(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        v = v.toLowerCase(Locale.ROOT);
        if (v.equals("organization") || v.equals("company")) {
            v = "business";
        }
        return DONOR_TYPES.contains(v) ? v : "individual";
    }

    private static String normalizePaymentMethod(String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        v = v.toLowerCase(Locale.ROOT);
        if (v.equals("cheque")) {
            v = "check";
        }
        return PAYMENT_METHODS.contains(v) ? v : null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
