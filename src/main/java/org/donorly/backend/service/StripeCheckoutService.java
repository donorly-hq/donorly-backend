package org.donorly.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Payment;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Public Stripe donation flow: creates Checkout sessions for a campaign and
 * records the resulting payment when Stripe confirms it via webhook.
 *
 * <p>Runs on unauthenticated routes, so there is no tenant context — the
 * organization is derived from the campaign, and echoed back to the webhook
 * through session metadata.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCheckoutService {

    private static final BigDecimal MAX_ONLINE_DONATION = new BigDecimal("100000");

    private final StripeGateway stripeGateway;
    private final CampaignRepository campaignRepository;
    private final DonorRepository donorRepository;
    private final PaymentRepository paymentRepository;
    private final DonorMatchingService donorMatchingService;
    private final DonorLifetimeGivingService lifetimeGivingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${donorly.mail.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    public boolean isLive() {
        return stripeGateway.isLive();
    }

    /** Creates a Checkout session for a one-off campaign donation; returns the hosted URL. */
    public String checkout(UUID campaignId, BigDecimal amount, String donorName, String donorEmail) {
        if (!stripeGateway.isLive()) {
            throw new BadRequestException(
                    "Online payments are not active yet for this organization. Please pay a volunteer directly.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        if (amount.compareTo(MAX_ONLINE_DONATION) > 0) {
            throw new BadRequestException("For donations this large, please contact the organization directly");
        }
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        if (!"active".equals(campaign.getStatus())) {
            throw new BadRequestException("This campaign is not accepting donations right now");
        }

        long cents = amount.movePointRight(2).longValueExact();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("org_id", campaign.getOrganizationId().toString());
        metadata.put("campaign_id", campaign.getId().toString());
        metadata.put("donor_name", donorName);
        metadata.put("donor_email", donorEmail);

        String publicPage = appBaseUrl + "/p/" + campaignId;
        return stripeGateway.createCheckoutSession(
                "Donation — " + campaign.getName(),
                cents,
                "usd",
                publicPage + "?paid=1",
                publicPage + "?cancelled=1",
                donorEmail,
                metadata);
    }

    /**
     * Handles Stripe webhook events. On {@code checkout.session.completed} the
     * payment is recorded against the campaign from the session metadata; the
     * donor is matched by email/name or created as a new "potential" donor.
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!stripeGateway.verifyWebhookSignature(payload, signatureHeader)) {
            throw new BadRequestException("Invalid webhook signature");
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }
        String type = event.path("type").asText();
        if (!"checkout.session.completed".equals(type)) {
            log.debug("[Stripe] Ignoring webhook event type {}", type);
            return;
        }

        JsonNode session = event.path("data").path("object");
        String sessionId = session.path("id").asText();
        JsonNode metadata = session.path("metadata");
        UUID orgId = parseUuid(metadata.path("org_id").asText(null));
        UUID campaignId = parseUuid(metadata.path("campaign_id").asText(null));
        if (orgId == null || campaignId == null) {
            log.warn("[Stripe] checkout.session.completed without org/campaign metadata — ignoring ({})", sessionId);
            return;
        }
        // Idempotency: Stripe retries webhooks, so skip if we already recorded this session.
        if (paymentRepository.existsByOrganizationIdAndReference(orgId, sessionId)) {
            log.info("[Stripe] Session {} already recorded — skipping duplicate webhook", sessionId);
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(session.path("amount_total").asLong(0), 2);
        if (amount.signum() <= 0) {
            log.warn("[Stripe] Session {} has no positive amount — ignoring", sessionId);
            return;
        }

        String donorName = firstNonBlank(
                metadata.path("donor_name").asText(null),
                session.path("customer_details").path("name").asText(null),
                "Online donor");
        String donorEmail = firstNonBlank(
                metadata.path("donor_email").asText(null),
                session.path("customer_details").path("email").asText(null),
                null);

        Donor donor = donorMatchingService.findExistingDonor(orgId, donorName, donorEmail, null);
        if (donor == null) {
            donor = new Donor();
            donor.setOrganizationId(orgId);
            donor.setFullName(donorName);
            donor.setEmail(donorEmail);
            donor.setBucket("potential");
            donor = donorRepository.save(donor);
        }

        Payment payment = new Payment();
        payment.setOrganizationId(orgId);
        payment.setCampaignId(campaignId);
        payment.setDonorId(donor.getId());
        payment.setAmount(amount);
        payment.setPaymentMethod("card");
        payment.setPaymentDate(LocalDate.now());
        payment.setReference(sessionId);
        payment.setNotes("Stripe Checkout donation");
        paymentRepository.save(payment);

        lifetimeGivingService.recompute(orgId, donor.getId());
        log.info("[Stripe] Recorded {} donation for campaign {} (session {})", amount, campaignId, sessionId);
    }

    private static UUID parseUuid(String value) {
        try {
            return value != null ? UUID.fromString(value) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
