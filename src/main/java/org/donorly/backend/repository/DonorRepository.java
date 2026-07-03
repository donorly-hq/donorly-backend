package org.donorly.backend.repository;

import org.donorly.backend.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorRepository extends JpaRepository<Donor, UUID> {
    List<Donor> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
    Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
}
