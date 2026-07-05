package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.PledgeRequest;
import org.donorly.backend.dto.PledgeUpdateRequest;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PledgeService {

    private final PledgeRepository pledgeRepository;
    private final CampaignRepository campaignRepository;
    private final DonorRepository donorRepository;
    private final AuditService auditService;

    public List<Pledge> list() {
        return pledgeRepository.findByOrganizationId(TenantContext.requireOrganizationId());
    }

    public org.donorly.backend.dto.PageResponse<Pledge> page(int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0), DonorService.clampSize(size),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return org.donorly.backend.dto.PageResponse.from(
                pledgeRepository.findByOrganizationId(TenantContext.requireOrganizationId(), pageable));
    }

    public List<Pledge> listByCampaign(UUID campaignId) {
        return pledgeRepository.findByOrganizationIdAndCampaignId(TenantContext.requireOrganizationId(), campaignId);
    }

    public List<Pledge> listByDonor(UUID donorId) {
        return pledgeRepository.findByOrganizationIdAndDonorId(TenantContext.requireOrganizationId(), donorId);
    }

    public Pledge get(UUID id) {
        return pledgeRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Pledge not found"));
    }

    @Transactional
    public Pledge create(UUID campaignId, PledgeRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        donorRepository.findByIdAndOrganizationId(request.donorId(), orgId)
                .orElseThrow(() -> new NotFoundException("Donor not found"));

        Pledge pledge = new Pledge();
        pledge.setOrganizationId(orgId);
        pledge.setCampaignId(campaignId);
        pledge.setDonorId(request.donorId());
        pledge.setAmount(request.amount());
        if (request.frequency() != null) {
            pledge.setFrequency(request.frequency());
        }
        pledge.setPaymentMethod(request.paymentMethod());
        pledge.setStartDate(request.startDate());
        pledge.setEndDate(request.endDate());
        pledge.setSource(request.source());
        pledge.setNotes(request.notes());
        pledge.setStatus("pending");
        pledge.setCreatedBy(TenantContext.getUserId());

        Pledge saved = pledgeRepository.save(pledge);
        auditService.record("pledge.create", "pledge", saved.getId());
        return saved;
    }

    @Transactional
    public Pledge update(UUID id, PledgeUpdateRequest request) {
        Pledge pledge = get(id);
        if (request.amount() != null) {
            pledge.setAmount(request.amount());
        }
        if (request.collectedAmount() != null) {
            pledge.setCollectedAmount(request.collectedAmount());
        }
        if (request.status() != null) {
            pledge.setStatus(request.status());
        }
        if (request.paymentMethod() != null) {
            pledge.setPaymentMethod(request.paymentMethod());
        }
        if (request.notes() != null) {
            pledge.setNotes(request.notes());
        }
        Pledge saved = pledgeRepository.save(pledge);
        recomputeDonorLifetimeGiving(pledge.getOrganizationId(), pledge.getDonorId());
        auditService.record("pledge.update", "pledge", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Pledge pledge = get(id);
        pledgeRepository.delete(pledge);
        recomputeDonorLifetimeGiving(pledge.getOrganizationId(), pledge.getDonorId());
        auditService.record("pledge.delete", "pledge", id);
    }

    /**
     * Quick entry for live events: finds an existing donor by email or
     * normalized name+phone, creates one otherwise, and records the pledge
     * in a single call.
     */
    @Transactional
    public org.donorly.backend.dto.QuickPledgeResponse quickCreate(org.donorly.backend.dto.QuickPledgeRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        campaignRepository.findByIdAndOrganizationId(request.campaignId(), orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        String name = request.donorName().trim();
        String email = normalize(request.email());
        String phone = normalize(request.phone());

        Donor donor = findExistingDonor(orgId, name, email, phone);
        boolean newDonor = donor == null;
        if (newDonor) {
            donor = new Donor();
            donor.setOrganizationId(orgId);
            donor.setFullName(name);
            donor.setEmail(email);
            donor.setPhone(phone);
            donor = donorRepository.save(donor);
        }

        Pledge pledge = new Pledge();
        pledge.setOrganizationId(orgId);
        pledge.setCampaignId(request.campaignId());
        pledge.setDonorId(donor.getId());
        pledge.setAmount(request.amount());
        pledge.setPaymentMethod(request.paymentMethod());
        pledge.setStartDate(java.time.LocalDate.now());
        pledge.setSource("live_event");
        pledge.setStatus("pending");
        pledge.setCreatedBy(TenantContext.getUserId());
        Pledge saved = pledgeRepository.save(pledge);

        auditService.record("pledge.quick_create", "pledge", saved.getId());
        return new org.donorly.backend.dto.QuickPledgeResponse(
                saved.getId(), donor.getId(), donor.getFullName(), saved.getAmount(), newDonor);
    }

    /** Live tally snapshot for the projector screen. */
    public org.donorly.backend.dto.CampaignLiveResponse liveProgress(UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        var campaign = campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        BigDecimal pledged = pledgeRepository.sumPledgedByCampaign(orgId, campaignId);
        BigDecimal collected = pledgeRepository.sumCollectedByCampaign(orgId, campaignId);
        int pledgeCount = pledgeRepository.findByOrganizationIdAndCampaignId(orgId, campaignId).size();

        var recent = pledgeRepository.findTop10ByOrganizationIdAndCampaignIdOrderByCreatedAtDesc(orgId, campaignId);
        var donorNames = donorRepository.findAllById(
                        recent.stream().map(Pledge::getDonorId).collect(java.util.stream.Collectors.toSet()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Donor::getId, Donor::getFullName));

        var recentDtos = recent.stream()
                .map(p -> new org.donorly.backend.dto.CampaignLiveResponse.RecentPledge(
                        donorNames.getOrDefault(p.getDonorId(), "Anonymous"),
                        p.getAmount(), p.getCreatedAt()))
                .toList();

        return new org.donorly.backend.dto.CampaignLiveResponse(
                campaign.getId(), campaign.getName(), campaign.getGoalAmount(),
                pledged != null ? pledged : BigDecimal.ZERO,
                collected != null ? collected : BigDecimal.ZERO,
                pledgeCount, recentDtos);
    }

    private Donor findExistingDonor(UUID orgId, String name, String email, String phone) {
        List<Donor> candidates = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
        if (email != null) {
            for (Donor d : candidates) {
                if (d.getEmail() != null && d.getEmail().equalsIgnoreCase(email)) {
                    return d;
                }
            }
        }
        String nameKey = DonorImportService.normalizeName(name);
        String phoneKey = DonorImportService.normalizePhone(phone);
        for (Donor d : candidates) {
            if (DonorImportService.normalizeName(d.getFullName()).equals(nameKey)
                    && DonorImportService.normalizePhone(d.getPhone()).equals(phoneKey)) {
                return d;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recomputeDonorLifetimeGiving(UUID orgId, UUID donorId) {
        BigDecimal collected = pledgeRepository.findByOrganizationIdAndDonorId(orgId, donorId).stream()
                .map(Pledge::getCollectedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        donorRepository.findByIdAndOrganizationId(donorId, orgId).ifPresent(donor -> {
            donor.setLifetimeGiving(collected);
            donorRepository.save(donor);
        });
    }
}
