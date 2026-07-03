package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.AssignmentRequest;
import org.donorly.backend.dto.AssignmentResponse;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorAssignment;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.DonorAssignmentRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonorAssignmentService {

    private final DonorAssignmentRepository assignmentRepository;
    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditService auditService;

    public List<AssignmentResponse> listForDonor(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireDonor(donorId, orgId);
        List<AssignmentResponse> result = new ArrayList<>();
        for (DonorAssignment a : assignmentRepository.findByOrganizationIdAndDonorId(orgId, donorId)) {
            result.add(toResponse(a));
        }
        return result;
    }

    @Transactional
    public AssignmentResponse assign(UUID donorId, AssignmentRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        Donor donor = requireDonor(donorId, orgId);

        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserId(orgId, request.ambassadorUserId())
                .orElseThrow(() -> new BadRequestException("That user is not a member of this organization"));
        if (!"active".equals(membership.getStatus())) {
            throw new BadRequestException("That user's membership is not active");
        }

        boolean exists = assignmentRepository.findByOrganizationIdAndDonorId(orgId, donorId).stream()
                .anyMatch(a -> a.getAmbassadorUserId().equals(request.ambassadorUserId())
                        && "active".equals(a.getStatus()));
        if (exists) {
            throw new ConflictException("This donor is already assigned to that ambassador");
        }

        DonorAssignment assignment = new DonorAssignment();
        assignment.setOrganizationId(orgId);
        assignment.setDonorId(donorId);
        assignment.setAmbassadorUserId(request.ambassadorUserId());
        assignment.setCampaignId(request.campaignId());
        assignment.setStatus("active");
        DonorAssignment saved = assignmentRepository.save(assignment);
        auditService.record("donor.assign", "donor", donor.getId());
        return toResponse(saved);
    }

    @Transactional
    public void unassign(UUID donorId, UUID assignmentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        DonorAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getOrganizationId().equals(orgId) && a.getDonorId().equals(donorId))
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        assignmentRepository.delete(assignment);
        auditService.record("donor.unassign", "donor", donorId);
    }

    public List<Donor> myDonors() {
        UUID orgId = TenantContext.requireOrganizationId();
        UUID userId = TenantContext.getUserId();
        List<Donor> donors = new ArrayList<>();
        for (DonorAssignment a : assignmentRepository.findByOrganizationIdAndAmbassadorUserId(orgId, userId)) {
            if (!"active".equals(a.getStatus())) {
                continue;
            }
            donorRepository.findByIdAndOrganizationId(a.getDonorId(), orgId)
                    .filter(d -> d.getDeletedAt() == null)
                    .ifPresent(donors::add);
        }
        return donors;
    }

    private Donor requireDonor(UUID donorId, UUID orgId) {
        return donorRepository.findByIdAndOrganizationId(donorId, orgId)
                .orElseThrow(() -> new NotFoundException("Donor not found"));
    }

    private AssignmentResponse toResponse(DonorAssignment a) {
        String donorName = donorRepository.findById(a.getDonorId()).map(Donor::getFullName).orElse(null);
        String ambassadorName = userRepository.findById(a.getAmbassadorUserId()).map(User::getFullName).orElse(null);
        return new AssignmentResponse(a.getId(), a.getDonorId(), donorName,
                a.getAmbassadorUserId(), ambassadorName, a.getCampaignId(), a.getStatus(), a.getCreatedAt());
    }
}
