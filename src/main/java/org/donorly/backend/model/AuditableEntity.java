package org.donorly.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all auditable entities.
 *
 * Provides four standard audit columns automatically maintained by
 * {@link AuditEntityListener}:
 * <ul>
 *   <li>{@code created_at}  — timestamp of initial insert</li>
 *   <li>{@code created_by}  — user UUID who created the record</li>
 *   <li>{@code modified_at} — timestamp of last update</li>
 *   <li>{@code modified_by} — user UUID who last modified the record</li>
 * </ul>
 *
 * Extend this class in every entity instead of declaring these fields
 * individually. The listener will fill them automatically from
 * {@link org.donorly.backend.tenant.TenantContext}.
 */
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;
}
