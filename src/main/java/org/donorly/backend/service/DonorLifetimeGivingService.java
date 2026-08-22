package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Owns the denormalized {@code donors.lifetime_giving} column: the sum of the
 * donor's pledge collectedAmounts plus direct (no-pledge) campaign donations.
 * Pledge-linked payments are counted through collectedAmount, so the two parts
 * never double-count. Extracted so payments and pledge edits use one recompute
 * implementation instead of duplicating it.
 */
@Service
@RequiredArgsConstructor
public class DonorLifetimeGivingService {

    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;
    private final org.donorly.backend.repository.PaymentRepository paymentRepository;

    public void recompute(UUID orgId, UUID donorId) {
        BigDecimal collected = pledgeRepository.findByOrganizationIdAndDonorId(orgId, donorId).stream()
                .map(Pledge::getCollectedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal direct = paymentRepository
                .findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(orgId, donorId).stream()
                .filter(p -> p.getPledgeId() == null)
                .map(org.donorly.backend.model.Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        donorRepository.findByIdAndOrganizationId(donorId, orgId).ifPresent(donor -> {
            donor.setLifetimeGiving(collected.add(direct));
            donorRepository.save(donor);
        });
    }
}
