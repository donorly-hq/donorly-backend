package org.donorly.backend.repository;

import org.donorly.backend.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByOrganizationId(UUID organizationId);
    List<Campaign> findByOrganizationIdAndStatusOrderByStartDateAsc(UUID organizationId, String status);
    List<Campaign> findByOrganizationIdAndManagedByUserId(UUID organizationId, UUID managedByUserId);
    Optional<Campaign> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);
}
