package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {

    Optional<TenantUser> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    // A user may belong to multiple tenants — use this at login to
    // find which orgs they can access, e.g. for an org-switcher UI.
    List<TenantUser> findByUserId(UUID userId);
}
