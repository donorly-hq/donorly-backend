package org.donorly.backend.repository;

import org.donorly.backend.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, UUID> {
    Optional<DonorProfile> findByDonorIdAndOrganizationId(UUID donorId, UUID organizationId);
}
