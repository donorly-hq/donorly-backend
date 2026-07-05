package org.donorly.backend.repository;

import org.donorly.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("select max(a.createdAt) from AuditLog a where a.organizationId = :orgId")
    Instant findLastActivityAt(@Param("orgId") UUID orgId);
}
