package org.donorly.backend.repository;

import org.donorly.backend.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    List<EventRegistration> findByOrganizationIdAndEventIdOrderByGuestNameAsc(UUID organizationId, UUID eventId);
    Optional<EventRegistration> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<EventRegistration> findByEventIdAndCheckInCode(UUID eventId, String checkInCode);
    long countByEventId(UUID eventId);
    long countByEventIdAndStatus(UUID eventId, String status);
}
