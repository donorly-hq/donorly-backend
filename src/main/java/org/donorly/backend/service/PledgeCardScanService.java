package org.donorly.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.dto.PledgeCardScanRequest;
import org.donorly.backend.dto.PledgeCardScanResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns a photo of a paper pledge card into a pre-filled form the user can verify.
 *
 * The model only sees pixels, so the prompt "grounds" it with the org's real campaign
 * names, and after extraction we match the donor against existing records with the
 * same {@link DonorMatchingService} logic that quick-pledge uses. The user always
 * reviews the result before anything is saved (approve-to-execute).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PledgeCardScanService {

    /** ~1.5 MB of base64 ≈ a 1600px JPEG with plenty of headroom. */
    private static final int MAX_IMAGE_CHARS = 2_000_000;

    private static final Set<String> PAYMENT_METHODS = Set.of("cash", "check", "card");

    private final AiGateway aiGateway;
    private final OrganizationSettingsRepository settingsRepository;
    private final CampaignRepository campaignRepository;
    private final DonorMatchingService donorMatchingService;
    private final CampaignMatchingService campaignMatchingService;

    // No ObjectMapper bean is exposed in this app; construct one like AiGateway does.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PledgeCardScanResponse scan(PledgeCardScanRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();

        boolean aiEnabled = settingsRepository.findById(orgId)
                .map(OrganizationSettings::isAiEnabled)
                .orElse(false);
        if (!aiEnabled) {
            throw new BadRequestException("AI is not enabled for this organization. Turn it on in Settings.");
        }

        String image = request.imageDataUrl();
        if (!image.startsWith("data:image/")) {
            throw new BadRequestException("Expected an image data URL (data:image/...).");
        }
        if (image.length() > MAX_IMAGE_CHARS) {
            throw new BadRequestException("Photo is too large. Please retake or use a smaller image.");
        }

        List<Campaign> campaigns = campaignRepository.findByOrganizationId(orgId);
        String campaignNames = campaigns.stream()
                .map(Campaign::getName)
                .collect(Collectors.joining("; "));

        String system = """
                You extract structured data from photos of handwritten or printed donation pledge cards.
                Reply with a single JSON object using exactly these keys:
                  donor_full_name (string), donor_email (string), donor_phone (string),
                  donor_city (string), donor_type ("individual" or "organization"),
                  amount (number, no currency symbols), payment_method ("cash", "check" or "card"),
                  campaign_name (string), notes (string).
                Use null for any field you cannot read confidently — never guess, especially the amount.
                If a campaign is named on the card, set campaign_name to the closest match from the
                known campaign list you are given; otherwise null.
                Put any other legible remarks from the card into notes.
                """;

        String user = campaignNames.isBlank()
                ? "Extract the pledge card fields from this photo. There are no known campaigns."
                : "Extract the pledge card fields from this photo. Known campaigns: " + campaignNames;

        String raw;
        try {
            raw = aiGateway.extractJsonFromImage(system, user, image);
        } catch (AiGateway.AiUnavailableException e) {
            throw new BadRequestException(e.getMessage());
        }

        JsonNode node = parseJson(raw);

        String donorName = text(node, "donor_full_name");
        String donorEmail = text(node, "donor_email");
        String donorPhone = text(node, "donor_phone");

        Campaign matchedCampaign = campaignMatchingService.match(campaigns, text(node, "campaign_name"));

        Donor matchedDonor = null;
        if (donorName != null && !donorName.isBlank()) {
            matchedDonor = donorMatchingService.findExistingDonor(orgId, donorName, donorEmail, donorPhone);
        }

        return new PledgeCardScanResponse(
                donorName,
                donorEmail,
                donorPhone,
                text(node, "donor_city"),
                normalizeDonorType(text(node, "donor_type")),
                amount(node),
                normalizePaymentMethod(text(node, "payment_method")),
                matchedCampaign != null ? matchedCampaign.getId() : null,
                matchedCampaign != null ? matchedCampaign.getName() : text(node, "campaign_name"),
                matchedDonor != null ? matchedDonor.getId() : null,
                matchedDonor != null ? matchedDonor.getFullName() : null,
                text(node, "notes"),
                node.toString()
        );
    }

    private JsonNode parseJson(String raw) {
        // json_object mode should return bare JSON, but strip code fences defensively.
        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```\\s*$", "");
        }
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("[AI] Could not parse pledge card extraction as JSON: {}", raw);
            throw new BadRequestException("AI could not read this photo. Try a clearer, well-lit picture of the card.");
        }
    }

    private String normalizePaymentMethod(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip().toLowerCase();
        if (v.equals("cheque")) {
            v = "check";
        }
        return PAYMENT_METHODS.contains(v) ? v : null;
    }

    private String normalizeDonorType(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip().toLowerCase();
        return v.equals("organization") ? "organization" : "individual";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asText().strip();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    private BigDecimal amount(JsonNode node) {
        JsonNode value = node.get("amount");
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        try {
            // Tolerate "1,500" or "$1500" if the model slips a string through.
            return new BigDecimal(value.asText().replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
