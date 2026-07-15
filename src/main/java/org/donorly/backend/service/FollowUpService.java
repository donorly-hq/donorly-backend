package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.FollowUpRequest;
import org.donorly.backend.dto.FollowUpUpdateRequest;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditService auditService;

    public List<FollowUp> list() {
        return followUpRepository.findByOrganizationId(TenantContext.requireOrganizationId());
    }

    public List<FollowUp> listByStatus(String status) {
        return followUpRepository.findByOrganizationIdAndStatus(TenantContext.requireOrganizationId(), status);
    }

    public List<FollowUp> listMine() {
        return followUpRepository.findByOrganizationIdAndAssignedToUserId(
                TenantContext.requireOrganizationId(), TenantContext.getUserId());
    }

    public FollowUp get(UUID id) {
        return followUpRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Follow-up not found"));
    }

    @Transactional
    public FollowUp create(FollowUpRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        // Every foreign key from the request body must belong to the caller's org,
        // otherwise a client could link a follow-up to another tenant's donor/campaign/user.
        if (request.donorId() != null
                && donorRepository.findByIdAndOrganizationId(request.donorId(), orgId).isEmpty()) {
            throw new BadRequestException("Donor not found in this organization");
        }
        if (request.campaignId() != null
                && campaignRepository.findByIdAndOrganizationId(request.campaignId(), orgId).isEmpty()) {
            throw new BadRequestException("Campaign not found in this organization");
        }
        if (request.assignedToUserId() != null
                && membershipRepository.findByOrganizationIdAndUserId(orgId, request.assignedToUserId()).isEmpty()) {
            throw new BadRequestException("Assigned user is not a member of this organization");
        }

        FollowUp followUp = new FollowUp();
        followUp.setOrganizationId(orgId);
        followUp.setDonorId(request.donorId());
        followUp.setCampaignId(request.campaignId());
        followUp.setAssignedToUserId(request.assignedToUserId());
        followUp.setDueAt(request.dueAt());
        followUp.setNotes(request.notes());
        followUp.setStatus("open");
        FollowUp saved = followUpRepository.save(followUp);
        auditService.record("followup.create", "follow_up", saved.getId());
        return saved;
    }

    @Transactional
    public FollowUp update(UUID id, FollowUpUpdateRequest request) {
        FollowUp followUp = get(id);
        if (request.status() != null) {
            followUp.setStatus(request.status());
        }
        if (request.outcome() != null) {
            followUp.setOutcome(request.outcome());
        }
        if (request.notes() != null) {
            followUp.setNotes(request.notes());
        }
        if (request.dueAt() != null) {
            followUp.setDueAt(request.dueAt());
        }
        FollowUp saved = followUpRepository.save(followUp);
        auditService.record("followup.update", "follow_up", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        FollowUp followUp = get(id);
        followUpRepository.delete(followUp);
        auditService.record("followup.delete", "follow_up", id);
    }
}
