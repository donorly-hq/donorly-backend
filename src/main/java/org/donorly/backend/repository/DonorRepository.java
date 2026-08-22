package org.donorly.backend.repository;

import org.donorly.backend.model.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorRepository extends JpaRepository<Donor, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Donor> {
    List<Donor> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
    Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    /**
     * Cross-org lookup by phone digits for inbound Twilio webhooks (no tenant
     * context on public routes). Numbers are compared digits-only so formatting
     * differences ("+1 (555) ..." vs "555...") still match.
     */
    @Query(value = """
            select * from donors d
            where d.deleted_at is null
              and regexp_replace(coalesce(d.phone, ''), '\\D', '', 'g') <> ''
              and regexp_replace(coalesce(d.phone, ''), '\\D', '', 'g')
                  = regexp_replace(:digits, '\\D', '', 'g')
            """, nativeQuery = true)
    List<Donor> findByPhoneDigits(@Param("digits") String digits);

    /** Past givers with no payment since {@code cutoff} — lapsed-donor signal. */
    @Query("""
            select count(d) from Donor d
            where d.organizationId = :orgId and d.deletedAt is null
              and d.lifetimeGiving > 0
              and not exists (
                  select 1 from Payment p
                  where p.donorId = d.id and p.organizationId = :orgId and p.createdAt >= :cutoff
              )
            """)
    long countDormantSince(@Param("orgId") UUID orgId, @Param("cutoff") java.time.Instant cutoff);

    @Query("""
            select d from Donor d
            where d.organizationId = :orgId and d.deletedAt is null
              and (lower(d.fullName) like :q
                   or lower(coalesce(d.email, '')) like :q
                   or lower(coalesce(d.phone, '')) like :q
                   or lower(coalesce(d.city, '')) like :q)
            """)
    Page<Donor> pageByOrganization(@Param("orgId") UUID orgId, @Param("q") String q, Pageable pageable);

    @Query("""
            select d from Donor d
            where d.organizationId = :orgId and d.deletedAt is null
              and d.id in :ids
              and (lower(d.fullName) like :q
                   or lower(coalesce(d.email, '')) like :q
                   or lower(coalesce(d.phone, '')) like :q
                   or lower(coalesce(d.city, '')) like :q)
            """)
    Page<Donor> pageByOrganizationAndIds(@Param("orgId") UUID orgId, @Param("q") String q,
                                         @Param("ids") Collection<UUID> ids, Pageable pageable);
}
