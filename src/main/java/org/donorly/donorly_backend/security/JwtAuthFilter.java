package org.donorly.donorly_backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.donorly.donorly_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parseClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);
                String ambassadorId = claims.get("ambassadorId", String.class);
                String jti = claims.getId();

                userRepository.findById(userId).ifPresent(user -> {
                    // Single-session enforcement for Ambassadors: the token's jti
                    // must match what's recorded as the active session. If a newer
                    // login has happened, this token is stale and gets rejected.
                    boolean sessionValid = !"AMBASSADOR".equals(user.getRole())
                            || jti.equals(user.getActiveSessionToken());

                    boolean isActive = user.getActive() == null || user.getActive();

                    if (sessionValid && isActive) {
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        var authToken = new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(userId, role, ambassadorId), null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                });
            } catch (Exception ignored) {
                // invalid/expired token — leave context unauthenticated; request gets rejected downstream
            }
        }

        chain.doFilter(request, response);
    }

    public record AuthenticatedUser(String userId, String role, String ambassadorId) {}
}
