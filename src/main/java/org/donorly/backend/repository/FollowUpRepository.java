package org.donorly.backend.repository;

import org.donorly.backend.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {
    List<FollowUp> findByOrganizationId(UUID organizationId);
    List<FollowUp> findByOrganizationIdAndStatus(UUID organizationId, String status);
    List<FollowUp> findByOrganizationIdAndAssignedToUserId(UUID organizationId, UUID assignedToUserId);
    Optional<FollowUp> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
    long countByOrganizationIdAndAssignedToUserIdAndStatus(UUID organizationId, UUID assignedToUserId, String status);
    long countByOrganizationIdAndAssignedToUserId(UUID organizationId, UUID assignedToUserId);
}
