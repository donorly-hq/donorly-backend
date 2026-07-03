package org.donorly.backend.repository;

import org.donorly.backend.model.DonorTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorTagRepository extends JpaRepository<DonorTag, UUID> {
    List<DonorTag> findByOrganizationIdOrderByName(UUID organizationId);
    Optional<DonorTag> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<DonorTag> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
