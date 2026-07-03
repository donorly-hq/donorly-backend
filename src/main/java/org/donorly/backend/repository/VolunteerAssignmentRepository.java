package org.donorly.backend.repository;

import org.donorly.backend.model.VolunteerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VolunteerAssignmentRepository extends JpaRepository<VolunteerAssignment, UUID> {
    List<VolunteerAssignment> findByShiftId(UUID shiftId);
    List<VolunteerAssignment> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    Optional<VolunteerAssignment> findByShiftIdAndUserId(UUID shiftId, UUID userId);
    Optional<VolunteerAssignment> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByShiftIdAndStatusNot(UUID shiftId, String status);
}
