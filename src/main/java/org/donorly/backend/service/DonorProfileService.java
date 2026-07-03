package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.DonorProfileRequest;
import org.donorly.backend.dto.DonorProfileResponse;
import org.donorly.backend.model.DonorProfile;
import org.donorly.backend.repository.DonorProfileRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonorProfileService {

    private final DonorProfileRepository profileRepository;
    private final DonorService donorService;
    private final AuditService auditService;

    public DonorProfileResponse getProfile(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);
        return profileRepository.findByDonorIdAndOrganizationId(donorId, orgId)
                .map(this::toResponse)
                .orElse(new DonorProfileResponse(donorId, null, null, null, null, null));
    }

    @Transactional
    public DonorProfileResponse upsertProfile(UUID donorId, DonorProfileRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        donorService.get(donorId);

        DonorProfile profile = profileRepository.findByDonorIdAndOrganizationId(donorId, orgId)
                .orElseGet(() -> {
                    DonorProfile p = new DonorProfile();
                    p.setDonorId(donorId);
                    p.setOrganizationId(orgId);
                    return p;
                });

        profile.setOccupation(request.occupation());
        profile.setEmployer(request.employer());
        profile.setPreferredLanguage(request.preferredLanguage());
        profile.setPreferredChannel(request.preferredChannel());
        profile.setNotesPrivate(request.notesPrivate());

        DonorProfile saved = profileRepository.save(profile);
        auditService.record("donor_profile.update", "donor", donorId);
        return toResponse(saved);
    }

    private DonorProfileResponse toResponse(DonorProfile profile) {
        return new DonorProfileResponse(
                profile.getDonorId(),
                profile.getOccupation(),
                profile.getEmployer(),
                profile.getPreferredLanguage(),
                profile.getPreferredChannel(),
                profile.getNotesPrivate()
        );
    }
}
