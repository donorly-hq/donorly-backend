package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.dto.LoginRequest;
import org.donorly.backend.dto.LoginResponse;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.Role;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.RoleRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BadRequestException("Account is not active");
        }

        Organization org = null;
        OrganizationMembership membership = null;
        Role role = null;

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());

        if (request.organizationSlug() != null && !request.organizationSlug().isBlank()) {
            org = organizationRepository.findBySlug(request.organizationSlug())
                    .orElseThrow(() -> new BadRequestException("Unknown organization"));
            final UUID orgId = org.getId();
            membership = memberships.stream()
                    .filter(m -> m.getOrganizationId().equals(orgId) && "active".equals(m.getStatus()))
                    .findFirst().orElse(null);
        } else {
            membership = memberships.stream()
                    .filter(m -> "active".equals(m.getStatus()))
                    .findFirst().orElse(null);
            if (membership != null) {
                org = organizationRepository.findById(membership.getOrganizationId()).orElse(null);
            }
        }

        if (!user.isPlatformAdmin() && membership == null) {
            throw new BadRequestException("No active organization membership");
        }

        if (membership != null) {
            role = roleRepository.findById(membership.getRoleId()).orElse(null);
        }

        String jti = UUID.randomUUID().toString();
        UUID orgId = org != null ? org.getId() : null;
        String token = jwtUtil.generateToken(user.getId(), orgId, user.isPlatformAdmin(), jti);

        user.setActiveSessionToken(jti);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> permissions = role != null
                ? role.getPermissions().stream().map(p -> p.getCode()).sorted().toList()
                : Collections.emptyList();

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.isPlatformAdmin(),
                orgId,
                org != null ? org.getName() : null,
                org != null ? org.getPrimaryColor() : null,
                org != null ? (org.getLogoData() != null ? org.getLogoData() : org.getLogoUrl()) : null,
                role != null ? role.getCode() : (user.isPlatformAdmin() ? "platform_super_admin" : null),
                permissions
        );
    }

    @Transactional
    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setActiveSessionToken(null);
            userRepository.save(user);
        });
    }
}
