package org.donorly.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.Role;
import org.donorly.backend.model.User;
import org.donorly.backend.model.UserSession;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.RoleRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.repository.UserSessionRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            String orgIdClaim = claims.get("orgId", String.class);
            boolean platformAdmin = Boolean.TRUE.equals(claims.get("platformAdmin", Boolean.class));
            String jti = claims.getId();

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !"active".equals(user.getStatus())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid account");
                return;
            }
            // Session must exist and be unexpired (multiple concurrent sessions allowed)
            UserSession session = jti != null ? sessionRepository.findById(jti).orElse(null) : null;
            if (session == null || !session.getUserId().equals(userId)
                    || session.getExpiresAt().isBefore(java.time.Instant.now())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                return;
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            UUID organizationId = null;

            if (platformAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("platform.organizations.manage"));
            }

            if (orgIdClaim != null) {
                organizationId = UUID.fromString(orgIdClaim);
                OrganizationMembership membership =
                        membershipRepository.findByOrganizationIdAndUserId(organizationId, userId).orElse(null);
                if (membership == null || !"active".equals(membership.getStatus())) {
                    if (!platformAdmin) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "No active membership for organization");
                        return;
                    }
                } else {
                    Role role = roleRepository.findById(membership.getRoleId()).orElse(null);
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));
                        role.getPermissions().forEach(p ->
                                authorities.add(new SimpleGrantedAuthority(p.getCode())));
                    }
                }
            }

            TenantContext.setUserId(userId);
            TenantContext.setOrganizationId(organizationId);

            // credentials carries the jti so logout can end just this session
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, jti, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } finally {
            TenantContext.clear();
        }
    }
}
