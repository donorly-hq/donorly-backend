package org.donorly.backend.repository;

import org.donorly.backend.model.Townhall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TownhallRepository extends JpaRepository<Townhall, UUID> {
    List<Townhall> findByOrganizationIdOrderByEventDateDescEventTimeDesc(UUID organizationId);
    List<Townhall> findByOrganizationIdAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(UUID organizationId, java.time.LocalDate from);
    Optional<Townhall> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
