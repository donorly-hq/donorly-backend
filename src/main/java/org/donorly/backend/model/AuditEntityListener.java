package org.donorly.backend.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.donorly.backend.tenant.TenantContext;

import java.time.Instant;

/**
 * JPA entity listener that automatically stamps every {@link AuditableEntity}
 * with {@code createdAt}/{@code createdBy} on insert and
 * {@code modifiedAt}/{@code modifiedBy} on every update.
 *
 * Uses {@link TenantContext} (thread-local) so no Spring context lookup is needed.
 */
public class AuditEntityListener {

    @PrePersist
    public void onPrePersist(AuditableEntity entity) {
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setModifiedAt(now);

        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(TenantContext.getUserId());
        }
        entity.setModifiedBy(TenantContext.getUserId());
    }

    @PreUpdate
    public void onPreUpdate(AuditableEntity entity) {
        entity.setModifiedAt(Instant.now());
        entity.setModifiedBy(TenantContext.getUserId());
    }
}
