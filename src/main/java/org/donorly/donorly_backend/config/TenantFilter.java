package org.donorly.donorly_backend.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs once per request, AFTER Spring Security has authenticated the
 * user. Resolves which tenant this request belongs to and sets the
 * Postgres session variable app.current_tenant_id, which is what the
 * RLS policies in the schema check against.
 *
 * IMPORTANT: do NOT put @Transactional on this class or its methods.
 * Spring proxies @Transactional beans via CGLIB, and for a Filter
 * that breaks Tomcat's filter instantiation (GenericFilterBean's
 * logger field ends up null, crashing on startup). Use
 * TransactionTemplate instead to scope just the query that needs a
 * transaction.
 *
 * TODO: replace the placeholder resolveTenantIdForRequest() below
 * with your real JWT-claim lookup once you decide how tenant_id is
 * carried in the token.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    public TenantFilter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            UUID tenantId = resolveTenantIdForRequest(request);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                transactionTemplate.executeWithoutResult(status ->
                        entityManager.createNativeQuery("SET app.current_tenant_id = :tenantId")
                                .setParameter("tenantId", tenantId.toString())
                                .executeUpdate()
                );
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolveTenantIdForRequest(HttpServletRequest request) {
        // Placeholder — wire this to your actual auth mechanism once
        // tenant_id is added as a JWT claim at login.
        return null;
    }
}
