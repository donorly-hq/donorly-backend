package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.CampaignRequest;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AuditService auditService;

    public List<Campaign> list() {
        return campaignRepository.findByOrganizationId(TenantContext.requireOrganizationId());
    }

    public Campaign get(UUID id) {
        return campaignRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    @Transactional
    public Campaign create(CampaignRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        Campaign campaign = new Campaign();
        campaign.setOrganizationId(orgId);
        campaign.setName(request.name());
        campaign.setSlug(resolveSlug(orgId, request.slug(), request.name()));
        if (request.campaignType() != null) {
            campaign.setCampaignType(request.campaignType());
        }
        campaign.setGoalAmount(request.goalAmount() != null ? request.goalAmount() : BigDecimal.ZERO);
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        if (request.status() != null) {
            campaign.setStatus(request.status());
        }
        campaign.setManagedByUserId(request.managedByUserId());
        Campaign saved = campaignRepository.save(campaign);
        auditService.record("campaign.create", "campaign", saved.getId());
        return saved;
    }

    @Transactional
    public Campaign update(UUID id, CampaignRequest request) {
        Campaign campaign = get(id);
        campaign.setName(request.name());
        if (request.campaignType() != null) {
            campaign.setCampaignType(request.campaignType());
        }
        if (request.goalAmount() != null) {
            campaign.setGoalAmount(request.goalAmount());
        }
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        if (request.status() != null) {
            campaign.setStatus(request.status());
        }
        if (request.managedByUserId() != null) {
            campaign.setManagedByUserId(request.managedByUserId());
        }
        Campaign saved = campaignRepository.save(campaign);
        auditService.record("campaign.update", "campaign", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Campaign campaign = get(id);
        campaignRepository.delete(campaign);
        auditService.record("campaign.delete", "campaign", id);
    }

    private String resolveSlug(UUID orgId, String requestedSlug, String name) {
        String base = (requestedSlug != null && !requestedSlug.isBlank() ? requestedSlug : name)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "campaign";
        }
        String slug = base;
        int suffix = 2;
        while (campaignRepository.existsByOrganizationIdAndSlug(orgId, slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}
