package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorRequest;
import org.donorly.backend.dto.PledgeCardFilter;
import org.donorly.backend.dto.PledgeCardRequest;
import org.donorly.backend.dto.PledgeCardResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PledgeCardService {

    /** Statuses a card can be moved to; approval triggers pledge conversion. */
    private static final List<String> VALID_STATUSES = List.of(
            "pending", "reviewed", "approved", "rejected",
            "needs_verification", "claims_paid", "non_responsive");

    private final PledgeCardRepository pledgeCardRepository;
    private final CampaignRepository campaignRepository;
    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;
    private final UserRepository userRepository;
    private final org.donorly.backend.repository.OrganizationSettingsRepository settingsRepository;
    private final DonorService donorService;
    private final AuditService auditService;

    /** Current auto-approve policy: {autoApprove, hours}. */
    public java.util.Map<String, Object> autoApprovePolicy() {
        return settingsRepository.findById(TenantContext.requireOrganizationId())
                .<java.util.Map<String, Object>>map(s -> java.util.Map.of(
                        "autoApprove", s.isPledgeCardAutoApprove(),
                        "hours", s.getPledgeCardAutoApproveHours()))
                .orElse(java.util.Map.of("autoApprove", true, "hours", 24));
    }

    @Transactional
    public java.util.Map<String, Object> updateAutoApprovePolicy(boolean autoApprove, Integer hours) {
        var settings = settingsRepository.findById(TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization settings not found"));
        settings.setPledgeCardAutoApprove(autoApprove);
        if (hours != null) {
            if (hours < 1 || hours > 168) {
                throw new BadRequestException("Auto-approve window must be between 1 and 168 hours");
            }
            settings.setPledgeCardAutoApproveHours(hours);
        }
        settingsRepository.save(settings);
        auditService.record("pledge_card.auto_approve_policy", "organization_settings",
                settings.getOrganizationId());
        return autoApprovePolicy();
    }

    /** Current automated reminder policy: {enabled, intervalDays, maxAttempts}. */
    public java.util.Map<String, Object> reminderPolicy() {
        return settingsRepository.findById(TenantContext.requireOrganizationId())
                .<java.util.Map<String, Object>>map(s -> java.util.Map.of(
                        "enabled", s.isPledgeCardRemindersEnabled(),
                        "intervalDays", s.getPledgeCardReminderIntervalDays(),
                        "maxAttempts", s.getPledgeCardReminderMax()))
                .orElse(java.util.Map.of("enabled", true, "intervalDays", 3, "maxAttempts", 3));
    }

    @Transactional
    public java.util.Map<String, Object> updateReminderPolicy(boolean enabled, Integer intervalDays,
                                                              Integer maxAttempts) {
        var settings = settingsRepository.findById(TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization settings not found"));
        settings.setPledgeCardRemindersEnabled(enabled);
        if (intervalDays != null) {
            if (intervalDays < 1 || intervalDays > 60) {
                throw new BadRequestException("Reminder interval must be between 1 and 60 days");
            }
            settings.setPledgeCardReminderIntervalDays(intervalDays);
        }
        if (maxAttempts != null) {
            if (maxAttempts < 1 || maxAttempts > 10) {
                throw new BadRequestException("Reminder attempts must be between 1 and 10");
            }
            settings.setPledgeCardReminderMax(maxAttempts);
        }
        settingsRepository.save(settings);
        auditService.record("pledge_card.reminder_policy", "organization_settings",
                settings.getOrganizationId());
        return reminderPolicy();
    }

    public List<PledgeCardResponse> list(PledgeCardFilter filter) {
        UUID orgId = TenantContext.requireOrganizationId();
        return pledgeCardRepository.findByOrganizationId(orgId).stream()
                .filter(card -> matches(card, filter, orgId))
                .map(this::toResponse)
                .toList();
    }

    public List<PledgeCardResponse> listPending() {
        return pledgeCardRepository
                .findByOrganizationIdAndVerificationStatus(TenantContext.requireOrganizationId(), "pending")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Cards parked for manual review: needs_verification, claims_paid, non_responsive. */
    public List<PledgeCardResponse> listNeedsVerification() {
        UUID orgId = TenantContext.requireOrganizationId();
        return pledgeCardRepository.findByOrganizationId(orgId).stream()
                .filter(c -> List.of("needs_verification", "claims_paid", "non_responsive")
                        .contains(c.getVerificationStatus()))
                .map(this::toResponse)
                .toList();
    }

    public List<PledgeCardResponse> listByDonor(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        return pledgeCardRepository.findByOrganizationId(orgId).stream()
                .filter(c -> donorId.equals(c.getDonorId()))
                .map(this::toResponse)
                .toList();
    }

    public PledgeCardResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public PledgeCardResponse create(PledgeCardRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();

        if (request.campaignId() != null) {
            campaignRepository.findByIdAndOrganizationId(request.campaignId(), orgId)
                    .orElseThrow(() -> new NotFoundException("Campaign not found"));
        }

        // Resolve donor: existing id takes priority, otherwise create from inline fields.
        UUID donorId = request.donorId();
        if (donorId != null) {
            donorRepository.findByIdAndOrganizationId(donorId, orgId)
                    .orElseThrow(() -> new NotFoundException("Donor not found"));
        } else if (request.donorFullName() != null && !request.donorFullName().isBlank()) {
            Donor newDonor = donorService.create(new DonorRequest(
                    request.donorFullName(),
                    request.donorEmail(),
                    request.donorPhone(),
                    request.donorCity(),
                    null,       // state
                    null,       // address
                    request.donorType(),
                    "active",
                    null,       // assignedToUserId
                    null,       // majorDonor
                    null,       // bucket
                    null        // complianceStatus
            ));
            donorId = newDonor.getId();
        }

        PledgeCard card = new PledgeCard();
        card.setOrganizationId(orgId);
        card.setCampaignId(request.campaignId());
        card.setDonorId(donorId);
        card.setImageUrl(request.imageUrl());
        card.setExtractedJson(request.extractedJson());
        card.setAmount(request.amount());
        card.setPaymentMethod(request.paymentMethod());
        card.setNotes(request.notes());
        card.setVerificationStatus("pending");
        card.setPendingSince(Instant.now());
        // Every card needs a POC to chase it; default to whoever entered the card.
        card.setPointOfContactUserId(request.pointOfContactUserId() != null
                ? request.pointOfContactUserId()
                : TenantContext.getUserId());
        card.setBatch(request.batch() != null && !request.batch().isBlank() ? request.batch().trim() : null);
        card.setCreatedBy(TenantContext.getUserId());

        PledgeCard saved = pledgeCardRepository.save(card);
        auditService.record("pledge_card.create", "pledge_card", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PledgeCardResponse updateStatus(UUID id, String status) {
        PledgeCard card = find(id);
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException("Invalid verification status");
        }
        if ("approved".equals(status)) {
            approveAndConvert(card, true);
        } else {
            card.setVerificationStatus(status);
            if ("pending".equals(status) && card.getPendingSince() == null) {
                card.setPendingSince(Instant.now());
            }
        }
        PledgeCard saved = pledgeCardRepository.save(card);
        auditService.record("pledge_card.status", "pledge_card", saved.getId());
        return toResponse(saved);
    }

    /**
     * Approving a card converts it into a real Pledge so campaign totals move.
     * Used by both the manual approve action and the 24h auto-approve sweep.
     *
     * @param strict when true (manual path) missing data raises an error;
     *               when false (scheduler path) the caller should park the card instead.
     * @return the created pledge, or null when conversion was not possible.
     */
    public Pledge approveAndConvert(PledgeCard card, boolean strict) {
        if (card.getCampaignId() == null || card.getDonorId() == null || card.getAmount() == null) {
            if (strict) {
                throw new BadRequestException(
                        "Cannot approve: card needs a campaign, donor and amount to become a pledge");
            }
            return null;
        }
        card.setVerificationStatus("approved");

        Pledge pledge = new Pledge();
        pledge.setOrganizationId(card.getOrganizationId());
        pledge.setCampaignId(card.getCampaignId());
        pledge.setDonorId(card.getDonorId());
        pledge.setAmount(card.getAmount());
        pledge.setPaymentMethod(card.getPaymentMethod());
        pledge.setSource("pledge_card");
        pledge.setNotes(card.getNotes());
        pledge.setStatus("pending");
        pledge.setCreatedBy(card.getPointOfContactUserId());
        return pledgeRepository.save(pledge);
    }

    /** Increment the follow-up counter (called after each chase attempt). */
    @Transactional
    public PledgeCardResponse recordFollowUp(UUID id) {
        PledgeCard card = find(id);
        card.setFollowUpCount(card.getFollowUpCount() + 1);
        PledgeCard saved = pledgeCardRepository.save(card);
        auditService.record("pledge_card.follow_up", "pledge_card", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PledgeCard card = find(id);
        pledgeCardRepository.delete(card);
        auditService.record("pledge_card.delete", "pledge_card", id);
    }

    private boolean matches(PledgeCard card, PledgeCardFilter filter, UUID orgId) {
        if (filter == null) return true;
        if (filter.status() != null && !filter.status().equals(card.getVerificationStatus())) return false;
        if (filter.batch() != null && !filter.batch().equalsIgnoreCase(card.getBatch())) return false;
        if (filter.minAmount() != null
                && (card.getAmount() == null || card.getAmount().compareTo(filter.minAmount()) < 0)) return false;
        if (filter.maxAmount() != null
                && (card.getAmount() == null || card.getAmount().compareTo(filter.maxAmount()) > 0)) return false;
        if (filter.enteredAfter() != null
                && (card.getCreatedAt() == null || card.getCreatedAt().isBefore(filter.enteredAfter()))) return false;
        if (filter.enteredBefore() != null
                && (card.getCreatedAt() == null || card.getCreatedAt().isAfter(filter.enteredBefore()))) return false;

        // Location and compliance live on the donor.
        if (filter.location() != null || filter.compliance() != null) {
            if (card.getDonorId() == null) return false;
            Donor donor = donorRepository.findByIdAndOrganizationId(card.getDonorId(), orgId).orElse(null);
            if (donor == null) return false;
            if (filter.location() != null) {
                String needle = filter.location().toLowerCase();
                boolean cityHit = donor.getCity() != null && donor.getCity().toLowerCase().contains(needle);
                boolean stateHit = donor.getState() != null && donor.getState().toLowerCase().contains(needle);
                if (!cityHit && !stateHit) return false;
            }
            if (filter.compliance() != null && !filter.compliance().equals(donor.getComplianceStatus())) {
                return false;
            }
        }
        return true;
    }

    private PledgeCard find(UUID id) {
        return pledgeCardRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Pledge card not found"));
    }

    private PledgeCardResponse toResponse(PledgeCard card) {
        String campaignName = null;
        String donorName = null;
        String pocName = null;
        if (card.getCampaignId() != null) {
            campaignName = campaignRepository.findById(card.getCampaignId())
                    .map(Campaign::getName).orElse(null);
        }
        if (card.getDonorId() != null) {
            donorName = donorRepository.findById(card.getDonorId())
                    .map(Donor::getFullName).orElse(null);
        }
        if (card.getPointOfContactUserId() != null) {
            pocName = userRepository.findById(card.getPointOfContactUserId())
                    .map(User::getFullName).orElse(null);
        }
        return new PledgeCardResponse(
                card.getId(),
                card.getCampaignId(),
                campaignName,
                card.getDonorId(),
                donorName,
                card.getImageUrl(),
                card.getAmount(),
                card.getPaymentMethod(),
                card.getNotes(),
                card.getVerificationStatus(),
                card.getPointOfContactUserId(),
                pocName,
                card.getFollowUpCount(),
                card.getPendingSince(),
                card.getBatch(),
                card.getCreatedBy(),
                card.getCreatedAt()
        );
    }
}
