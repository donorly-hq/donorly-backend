package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.Donor;
import org.donorly.backend.repository.DonorRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single source of truth for "is this the same donor?" — email match first, then
 * normalized name + phone. Used by staff quick-entry and the public self-pledge flow
 * (previously copy-pasted in PledgeService and PublicPortalService).
 */
@Service
@RequiredArgsConstructor
public class DonorMatchingService {

    private final DonorRepository donorRepository;

    /** Returns the matching donor in the org, or null when no candidate matches. */
    public Donor findExistingDonor(UUID orgId, String name, String email, String phone) {
        var candidates = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
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
}
