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
