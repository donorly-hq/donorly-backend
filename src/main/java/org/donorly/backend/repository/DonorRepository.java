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

public interface DonorRepository extends JpaRepository<Donor, UUID> {
    List<Donor> findByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
    Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

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
