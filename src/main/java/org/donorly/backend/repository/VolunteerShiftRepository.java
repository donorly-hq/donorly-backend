package org.donorly.backend.repository;

import org.donorly.backend.model.VolunteerShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VolunteerShiftRepository extends JpaRepository<VolunteerShift, UUID> {
    List<VolunteerShift> findByOrganizationIdAndEventIdOrderByStartsAtAsc(UUID organizationId, UUID eventId);
    Optional<VolunteerShift> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
