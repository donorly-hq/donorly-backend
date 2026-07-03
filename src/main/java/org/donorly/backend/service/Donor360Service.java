package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.DonorDetailResponse;
import org.donorly.backend.model.Donor;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class Donor360Service {

    private final DonorService donorService;
    private final DonorProfileService profileService;
    private final DonorTagService tagService;
    private final DonorNoteService noteService;
    private final DonorAssignmentService assignmentService;
    private final PledgeService pledgeService;
    private final PaymentService paymentService;
    private final PledgeCardService pledgeCardService;
    private final FollowUpRepository followUpRepository;

    public DonorDetailResponse getDetail(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        Donor donor = donorService.get(donorId);

        return new DonorDetailResponse(
                donor,
                profileService.getProfile(donorId),
                tagService.listDonorTags(donorId),
                noteService.listNotes(donorId),
                pledgeService.listByDonor(donorId),
                followUpRepository.findByOrganizationId(orgId).stream()
                        .filter(f -> donorId.equals(f.getDonorId()))
                        .toList(),
                assignmentService.listForDonor(donorId),
                paymentService.listByDonor(donorId),
                pledgeCardService.listByDonor(donorId)
        );
    }
}
