package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Owns the denormalized {@code donors.lifetime_giving} column: it is always the sum
 * of the donor's pledge collectedAmounts. Extracted so payments and pledge edits use
 * one recompute implementation instead of duplicating it (previously copy-pasted in
 * PaymentService and PledgeService).
 */
@Service
@RequiredArgsConstructor
public class DonorLifetimeGivingService {

    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;

    public void recompute(UUID orgId, UUID donorId) {
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
