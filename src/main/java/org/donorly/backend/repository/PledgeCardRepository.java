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
}
