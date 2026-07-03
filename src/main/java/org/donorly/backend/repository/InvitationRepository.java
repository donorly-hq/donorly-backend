package org.donorly.backend.repository;

import org.donorly.backend.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, String status);
    Optional<Invitation> findByTokenHash(String tokenHash);
    Optional<Invitation> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Invitation> findByOrganizationIdAndEmailIgnoreCaseAndStatus(UUID organizationId, String email, String status);
}
