package org.donorly.backend.repository;

import org.donorly.backend.model.PledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PledgeCardRepository extends JpaRepository<PledgeCard, UUID> {
    List<PledgeCard> findByOrganizationId(UUID organizationId);
    List<PledgeCard> findByOrganizationIdAndVerificationStatus(UUID organizationId, String verificationStatus);
    Optional<PledgeCard> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<PledgeCard> findByOrganizationIdAndDonorIdAndVerificationStatus(
            UUID organizationId, UUID donorId, String verificationStatus);
    /** Cards sitting in the pending queue longer than {@code cutoff} — 24h auto-approve sweep. */
    List<PledgeCard> findByVerificationStatusAndPendingSinceBefore(
            String verificationStatus, java.time.Instant cutoff);
    /** Reminder sweep candidates across all orgs (pending / needs_verification). */
    List<PledgeCard> findByVerificationStatusIn(java.util.Collection<String> statuses);
}
