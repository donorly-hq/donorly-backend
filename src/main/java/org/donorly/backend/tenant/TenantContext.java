package org.donorly.backend.tenant;

import java.util.UUID;

/**
 * Holds the current request's tenant (organization) and user identity.
 * Populated by the JWT filter and cleared at the end of each request.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        ORGANIZATION_ID.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return ORGANIZATION_ID.get();
    }

    public static UUID requireOrganizationId() {
        UUID id = ORGANIZATION_ID.get();
        if (id == null) {
            throw new IllegalStateException("No active organization in the current request context");
        }
        return id;
    }

    public static void setUserId(UUID userId) {
        USER_ID.set(userId);
    }

    public static UUID getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        ORGANIZATION_ID.remove();
        USER_ID.remove();
    }
}
