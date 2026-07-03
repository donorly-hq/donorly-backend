package org.donorly.backend.dto;

import org.donorly.backend.model.Donor;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.model.Pledge;

import java.util.List;

public record DonorDetailResponse(
        Donor donor,
        DonorProfileResponse profile,
        List<DonorTagResponse> tags,
        List<DonorNoteResponse> notes,
        List<Pledge> pledges,
        List<FollowUp> followUps,
        List<AssignmentResponse> assignments,
        List<PaymentResponse> payments,
        List<PledgeCardResponse> pledgeCards
) {
}
