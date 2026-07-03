package org.donorly.backend.repository;

import org.donorly.backend.model.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    List<OrganizationMembership> findByUserId(UUID userId);
    List<OrganizationMembership> findByOrganizationId(UUID organizationId);
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    List<OrganizationMembership> findByOrganizationIdAndCreatedBy(UUID organizationId, UUID createdBy);
}
