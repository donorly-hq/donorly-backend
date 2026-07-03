package org.donorly.backend.repository;

import org.donorly.backend.model.DonorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DonorAssignmentRepository extends JpaRepository<DonorAssignment, UUID> {
    List<DonorAssignment> findByOrganizationIdAndAmbassadorUserId(UUID organizationId, UUID ambassadorUserId);
    List<DonorAssignment> findByOrganizationIdAndDonorId(UUID organizationId, UUID donorId);
}
