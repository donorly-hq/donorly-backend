package org.donorly.backend.repository;

import org.donorly.backend.model.FollowUp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {
    List<FollowUp> findByOrganizationId(UUID organizationId);
    List<FollowUp> findByOrganizationIdAndStatus(UUID organizationId, String status);
    List<FollowUp> findByOrganizationIdAndAssignedToUserId(UUID organizationId, UUID assignedToUserId);
    Optional<FollowUp> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, String status);
    long countByOrganizationIdAndAssignedToUserIdAndStatus(UUID organizationId, UUID assignedToUserId, String status);
    long countByOrganizationIdAndAssignedToUserId(UUID organizationId, UUID assignedToUserId);

    @Query("""
            select count(f) from FollowUp f
            where f.organizationId = :orgId and f.status = 'open' and f.dueAt < :now
            """)
    long countOverdue(@Param("orgId") UUID orgId, @Param("now") java.time.Instant now);

    /** Open follow-ups, most urgent first; ones without a due date sort last. */
    @Query("""
            select f from FollowUp f
            where f.organizationId = :orgId and f.status = 'open'
            order by f.dueAt asc nulls last, f.createdAt asc
            """)
    List<FollowUp> findOpenByDueSoonest(@Param("orgId") UUID orgId, Pageable pageable);
}
