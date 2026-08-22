package org.donorly.backend.repository;

import org.donorly.backend.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByOrganizationId(UUID organizationId);
    List<Campaign> findByOrganizationIdAndStatusOrderByStartDateAsc(UUID organizationId, String status);
    /** Cross-org lookup for platform-wide schedulers (campaign messaging automation). */
    List<Campaign> findByStatus(String status);
    List<Campaign> findByOrganizationIdAndManagedByUserId(UUID organizationId, UUID managedByUserId);
    Optional<Campaign> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(c.goalAmount), 0) from Campaign c "
            + "where c.organizationId = :orgId and c.status = 'active'")
    java.math.BigDecimal sumActiveGoalByOrganization(
            @org.springframework.data.repository.query.Param("orgId") UUID orgId);
}
