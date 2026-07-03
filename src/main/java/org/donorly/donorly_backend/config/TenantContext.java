package org.donorly.donorly_backend.config;

import java.util.UUID;

/**
 * Holds the current request's tenant_id in a ThreadLocal so the
 * TenantFilter (set at request start) and the Hibernate interceptor
 * (set on every DB connection) can agree on which tenant is active.
 *
 * IMPORTANT: must be cleared at the end of every request (see
 * TenantFilter's finally block) or it will leak across requests on
 * a pooled thread — that would be a tenant isolation bug.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
