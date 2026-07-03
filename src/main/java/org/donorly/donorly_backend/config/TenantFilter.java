package org.donorly.donorly_backend.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs once per request, AFTER Spring Security has authenticated the
 * user. Resolves which tenant this request belongs to (from the JWT
 * claim / authenticated user's active tenant_user_id) and:
 *   1. Stores it in TenantContext for the rest of this thread.
 *   2. Sets the Postgres session variable app.current_tenant_id,
 *      which is what the RLS policies in V1__initial_schema.sql
 *      actually check against.
 *
 * TODO: replace the placeholder resolveTenantIdForRequest() below
 * with your real JWT-claim lookup once you decide how tenant_id is
 * carried in the token (recommended: add a "tenant_id" claim when
 * issuing the JWT at login, since a user can belong to >1 tenant).
 *
 * Register this in your SecurityConfig filter chain AFTER the JWT
 * auth filter, so SecurityContextHolder is already populated.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            UUID tenantId = resolveTenantIdForRequest(request);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                // This is the line that makes Postgres RLS actually filter rows.
                // Must run inside the same transaction/connection as the queries
                // that follow — hence @Transactional on this method.
                entityManager.createNativeQuery("SET app.current_tenant_id = :tenantId")
                        .setParameter("tenantId", tenantId.toString())
                        .executeUpdate();
            }
            filterChain.doFilter(request, response);
        } finally {
            // Critical: pooled threads are reused across requests.
            // Without this, tenant A's context could leak into tenant B's request.
            TenantContext.clear();
        }
    }

    private UUID resolveTenantIdForRequest(HttpServletRequest request) {
        // Placeholder — wire this to your actual auth mechanism.
        // Common pattern: extract "tenant_id" claim from the validated JWT
        // that your existing JWT auth filter already put into SecurityContext.
        //
        // Example once wired:
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth != null && auth.getPrincipal() instanceof YourUserPrincipal principal) {
        //     return principal.getActiveTenantId();
        // }
        return null;
    }
}
