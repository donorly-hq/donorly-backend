package org.donorly.backend.repository;

import org.donorly.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByOrganizationIdOrderByStartsAtDesc(UUID organizationId);
    List<Event> findByOrganizationIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(UUID organizationId, java.time.Instant from);
    Optional<Event> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
