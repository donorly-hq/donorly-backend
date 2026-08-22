package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.CampaignAudienceResponse;
import org.donorly.backend.dto.CampaignMessagingRequest;
import org.donorly.backend.dto.CampaignTargetRequest;
import org.donorly.backend.model.CampaignMessaging;
import org.donorly.backend.model.CampaignTarget;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorTag;
import org.donorly.backend.repository.CampaignMessagingRepository;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.CampaignTargetRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.DonorTagRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Campaign audience targeting and messaging configuration.
 *
 * <p>Targets are stored as selectors (donor, tag/group, or state) and resolved
 * to a distinct donor set on read, so the "donors targeted" count is always
 * live — adding a donor to a targeted group later automatically includes them.
 */
@Service
@RequiredArgsConstructor
public class CampaignAudienceService {

    private static final Set<String> FREQUENCIES = Set.of("daily", "every_2_days", "weekly");
    private static final Set<String> CHANNELS = Set.of("whatsapp", "sms", "robocall", "email");

    private final CampaignRepository campaignRepository;
    private final CampaignTargetRepository targetRepository;
    private final CampaignMessagingRepository messagingRepository;
    private final DonorRepository donorRepository;
    private final DonorTagRepository donorTagRepository;
    private final AuditService auditService;

    private UUID requireCampaign(UUID campaignId, UUID orgId) {
        campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        return campaignId;
    }

    // ===== audience targets =================================================

    public CampaignAudienceResponse audience(UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireCampaign(campaignId, orgId);

        List<CampaignTarget> targets = targetRepository.findByCampaignIdAndOrganizationId(campaignId, orgId);
        List<CampaignAudienceResponse.Target> rows = targets.stream().map(t -> {
            String donorName = t.getDonorId() != null
                    ? donorRepository.findById(t.getDonorId()).map(Donor::getFullName).orElse(null)
                    : null;
            String tagName = t.getTagId() != null
                    ? donorTagRepository.findById(t.getTagId()).map(DonorTag::getName).orElse(null)
                    : null;
            return new CampaignAudienceResponse.Target(
                    t.getId(), t.getDonorId(), donorName, t.getTagId(), tagName, t.getState());
        }).toList();

        int count = targetRepository.resolveTargetedDonorIds(campaignId, orgId).size();
        return new CampaignAudienceResponse(rows, count);
    }

    @Transactional
    public CampaignAudienceResponse addTarget(UUID campaignId, CampaignTargetRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireCampaign(campaignId, orgId);

        int selectors = (request.donorId() != null ? 1 : 0)
                + (request.tagId() != null ? 1 : 0)
                + (request.state() != null && !request.state().isBlank() ? 1 : 0);
        if (selectors != 1) {
            throw new BadRequestException("Provide exactly one of: donorId, tagId, state");
        }
        if (request.donorId() != null) {
            donorRepository.findByIdAndOrganizationId(request.donorId(), orgId)
                    .orElseThrow(() -> new NotFoundException("Donor not found"));
        }
        if (request.tagId() != null) {
            donorTagRepository.findByIdAndOrganizationId(request.tagId(), orgId)
                    .orElseThrow(() -> new NotFoundException("Group not found"));
        }

        boolean duplicate = targetRepository.findByCampaignIdAndOrganizationId(campaignId, orgId).stream()
                .anyMatch(t -> (request.donorId() != null && request.donorId().equals(t.getDonorId()))
                        || (request.tagId() != null && request.tagId().equals(t.getTagId()))
                        || (request.state() != null && t.getState() != null
                            && request.state().trim().equalsIgnoreCase(t.getState())));
        if (!duplicate) {
            CampaignTarget target = new CampaignTarget();
            target.setOrganizationId(orgId);
            target.setCampaignId(campaignId);
            target.setDonorId(request.donorId());
            target.setTagId(request.tagId());
            target.setState(request.state() != null && !request.state().isBlank()
                    ? request.state().trim() : null);
            targetRepository.save(target);
            auditService.record("campaign.target.add", "campaign", campaignId);
        }
        return audience(campaignId);
    }

    @Transactional
    public CampaignAudienceResponse removeTarget(UUID campaignId, UUID targetId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireCampaign(campaignId, orgId);
        targetRepository.findById(targetId)
                .filter(t -> orgId.equals(t.getOrganizationId()) && campaignId.equals(t.getCampaignId()))
                .ifPresent(t -> {
                    targetRepository.delete(t);
                    auditService.record("campaign.target.remove", "campaign", campaignId);
                });
        return audience(campaignId);
    }

    /** Resolved distinct donor ids the campaign targets (used by the scheduler). */
    public List<UUID> targetedDonorIds(UUID campaignId, UUID orgId) {
        return targetRepository.resolveTargetedDonorIds(campaignId, orgId);
    }

    // ===== messaging config =================================================

    public CampaignMessaging messaging(UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireCampaign(campaignId, orgId);
        return messagingRepository.findByCampaignIdAndOrganizationId(campaignId, orgId)
                .orElseGet(() -> defaultMessaging(campaignId, orgId));
    }

    private CampaignMessaging defaultMessaging(UUID campaignId, UUID orgId) {
        CampaignMessaging m = new CampaignMessaging();
        m.setCampaignId(campaignId);
        m.setOrganizationId(orgId);
        return m;
    }

    @Transactional
    public CampaignMessaging updateMessaging(UUID campaignId, CampaignMessagingRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireCampaign(campaignId, orgId);

        CampaignMessaging m = messagingRepository.findByCampaignIdAndOrganizationId(campaignId, orgId)
                .orElseGet(() -> defaultMessaging(campaignId, orgId));

        if (request.frequency() != null) {
            if (!FREQUENCIES.contains(request.frequency())) {
                throw new BadRequestException("Invalid frequency: " + request.frequency());
            }
            m.setFrequency(request.frequency());
        }
        if (request.channels() != null) {
            List<String> normalized = request.channels().stream()
                    .map(c -> c.trim().toLowerCase())
                    .filter(c -> !c.isEmpty())
                    .distinct()
                    .toList();
            for (String channel : normalized) {
                if (!CHANNELS.contains(channel)) {
                    throw new BadRequestException("Invalid channel: " + channel);
                }
            }
            m.setChannels(String.join(",", normalized));
        }
        m.setMessageContent(request.messageContent());
        m.setFlyerUrl(request.flyerUrl());
        m.setPaymentLink(request.paymentLink());
        if (request.personalized() != null) {
            m.setPersonalized(request.personalized());
        }
        CampaignMessaging saved = messagingRepository.save(m);
        auditService.record("campaign.messaging.update", "campaign", campaignId);
        return saved;
    }
}
