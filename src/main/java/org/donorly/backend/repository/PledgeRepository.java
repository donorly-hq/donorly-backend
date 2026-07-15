package org.donorly.backend.repository;

import jakarta.persistence.LockModeType;
import org.donorly.backend.model.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PledgeRepository extends JpaRepository<Pledge, UUID> {
    List<Pledge> findByOrganizationId(UUID organizationId);
    org.springframework.data.domain.Page<Pledge> findByOrganizationId(
            UUID organizationId, org.springframework.data.domain.Pageable pageable);
    long countByOrganizationId(UUID organizationId);
    List<Pledge> findByOrganizationIdAndCampaignId(UUID organizationId, UUID campaignId);
    List<Pledge> findByOrganizationIdAndDonorId(UUID organizationId, UUID donorId);
    Optional<Pledge> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Same lookup but takes a row-level write lock (SELECT ... FOR UPDATE) so two
     * concurrent payments against the same pledge cannot both read the old
     * collectedAmount and lose one update. The lock is held until the surrounding
     * transaction commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pledge p where p.id = :id and p.organizationId = :orgId")
    Optional<Pledge> findByIdAndOrganizationIdForUpdate(@Param("id") UUID id, @Param("orgId") UUID orgId);

    @Query("select coalesce(sum(p.amount), 0) from Pledge p where p.organizationId = :orgId")
    BigDecimal sumPledgedByOrganization(@Param("orgId") UUID orgId);

    @Query("select coalesce(sum(p.collectedAmount), 0) from Pledge p where p.organizationId = :orgId")
    BigDecimal sumCollectedByOrganization(@Param("orgId") UUID orgId);

    @Query("select coalesce(sum(p.amount), 0) from Pledge p where p.organizationId = :orgId and p.campaignId = :campaignId")
    BigDecimal sumPledgedByCampaign(@Param("orgId") UUID orgId, @Param("campaignId") UUID campaignId);

    @Query("select coalesce(sum(p.collectedAmount), 0) from Pledge p where p.organizationId = :orgId and p.campaignId = :campaignId")
    BigDecimal sumCollectedByCampaign(@Param("orgId") UUID orgId, @Param("campaignId") UUID campaignId);

    @Query("select coalesce(sum(p.amount), 0) from Pledge p where p.organizationId = :orgId and p.donorId in :donorIds")
    BigDecimal sumPledgedByDonors(@Param("orgId") UUID orgId, @Param("donorIds") java.util.Collection<UUID> donorIds);

    @Query("select coalesce(sum(p.collectedAmount), 0) from Pledge p where p.organizationId = :orgId and p.donorId in :donorIds")
    BigDecimal sumCollectedByDonors(@Param("orgId") UUID orgId, @Param("donorIds") java.util.Collection<UUID> donorIds);

    long countByOrganizationIdAndDonorIdIn(UUID organizationId, java.util.Collection<UUID> donorIds);

    /** Unfulfilled pledges (all orgs) that have not been reminded since {@code cutoff}. */
    @Query("""
            select p from Pledge p
            where p.status in ('pending', 'active')
              and p.collectedAmount < p.amount
              and p.createdAt < :minAge
              and (p.lastReminderAt is null or p.lastReminderAt < :cutoff)
            """)
    List<Pledge> findDueForReminder(@Param("minAge") java.time.Instant minAge,
                                    @Param("cutoff") java.time.Instant cutoff);

    List<Pledge> findTop10ByOrganizationIdAndCampaignIdOrderByCreatedAtDesc(UUID organizationId, UUID campaignId);
}
