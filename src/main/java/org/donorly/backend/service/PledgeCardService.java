package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorRequest;
import org.donorly.backend.dto.PledgeCardRequest;
import org.donorly.backend.dto.PledgeCardResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PledgeCardService {

    private final PledgeCardRepository pledgeCardRepository;
    private final CampaignRepository campaignRepository;
    private final DonorRepository donorRepository;
    private final DonorService donorService;
    private final AuditService auditService;

    public List<PledgeCardResponse> list() {
        return pledgeCardRepository.findByOrganizationId(TenantContext.requireOrganizationId()).stream()
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
                    request.donorType(),
                    "active"
            ));
            donorId = newDonor.getId();
        }

        PledgeCard card = new PledgeCard();
        card.setOrganizationId(orgId);
        card.setCampaignId(request.campaignId());
        card.setDonorId(donorId);
        card.setImageUrl(request.imageUrl());
        card.setAmount(request.amount());
        card.setPaymentMethod(request.paymentMethod());
        card.setNotes(request.notes());
        card.setVerificationStatus("pending");
        card.setCreatedBy(TenantContext.getUserId());

        PledgeCard saved = pledgeCardRepository.save(card);
        auditService.record("pledge_card.create", "pledge_card", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PledgeCardResponse updateStatus(UUID id, String status) {
        PledgeCard card = find(id);
        if (!List.of("pending", "reviewed", "approved", "rejected").contains(status)) {
            throw new BadRequestException("Invalid verification status");
        }
        card.setVerificationStatus(status);
        PledgeCard saved = pledgeCardRepository.save(card);
        auditService.record("pledge_card.status", "pledge_card", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PledgeCard card = find(id);
        pledgeCardRepository.delete(card);
        auditService.record("pledge_card.delete", "pledge_card", id);
    }

    private PledgeCard find(UUID id) {
        return pledgeCardRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Pledge card not found"));
    }

    private PledgeCardResponse toResponse(PledgeCard card) {
        String campaignName = null;
        String donorName = null;
        if (card.getCampaignId() != null) {
            campaignName = campaignRepository.findById(card.getCampaignId())
                    .map(Campaign::getName).orElse(null);
        }
        if (card.getDonorId() != null) {
            donorName = donorRepository.findById(card.getDonorId())
                    .map(Donor::getFullName).orElse(null);
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
                card.getCreatedBy(),
                card.getCreatedAt()
        );
    }
}
