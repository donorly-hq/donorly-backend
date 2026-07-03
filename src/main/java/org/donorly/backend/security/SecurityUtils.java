package org.donorly.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience helpers for reading the current request's granted authorities,
 * used where fine-grained scoping can't be expressed with a single
 * {@code @PreAuthorize} check (e.g. "see all donors" vs "see only assigned").
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (granted.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }
}
