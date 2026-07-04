package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.OrgMemberSummary;
import org.donorly.backend.dto.OrganizationRequest;
import org.donorly.backend.dto.OrganizationResponse;
import org.donorly.backend.dto.SetOwnerRequest;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<OrganizationResponse> listAll() {
        return organizationRepository.findAll()
                .stream()
                .filter(o -> o.getDeletedAt() == null)
                .map(o -> toResponse(o, findOwner(o.getId())))
                .toList();
    }

    public OrganizationResponse getById(UUID id) {
        Organization org = findActive(id);
        return toResponse(org, findOwner(id));
    }

    /** Active members of an org — for platform-admin owner transfer UI. */
    public List<OrgMemberSummary> listMembers(UUID orgId) {
        findActive(orgId);
        return membershipRepository.findByOrganizationId(orgId).stream()
                .filter(m -> "active".equals(m.getStatus()))
                .map(m -> {
                    User user = userRepository.findById(m.getUserId()).orElse(null);
                    if (user == null) return null;
                    Role role = roleRepository.findById(m.getRoleId()).orElse(null);
                    return new OrgMemberSummary(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            role != null ? role.getCode() : null,
                            role != null ? role.getName() : null,
                            m.getStatus()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(OrgMemberSummary::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.findBySlug(request.slug()).isPresent()) {
            throw new ConflictException("An organization with slug '" + request.slug() + "' already exists");
        }

        boolean hasOwner = request.ownerEmail() != null && !request.ownerEmail().isBlank();
        if (hasOwner) {
            if (request.ownerName() == null || request.ownerName().isBlank()) {
                throw new BadRequestException("Owner name is required when creating with an owner account");
            }
            if (!userRepository.existsByEmailIgnoreCase(request.ownerEmail())
                    && (request.ownerPassword() == null || request.ownerPassword().isBlank())) {
                throw new BadRequestException("Owner password is required when creating a new owner account");
            }
        }

        Organization org = new Organization();
        org.setName(request.name());
        org.setSlug(request.slug());
        org.setVertical(request.vertical() != null ? request.vertical() : "nonprofit");
        org.setTimezone(request.timezone() != null ? request.timezone() : "America/Chicago");
        org.setLogoUrl(request.logoUrl());
        org.setLogoData(request.logoData());
        org.setPrimaryColor(request.primaryColor());
        org.setStatus("trial");
        org = organizationRepository.save(org);

        if (!settingsRepository.existsById(org.getId())) {
            OrganizationSettings settings = new OrganizationSettings();
            settings.setOrganizationId(org.getId());
            settingsRepository.save(settings);
        }

        User owner = null;
        if (hasOwner) {
            owner = assignOwnerUser(org.getId(), request.ownerEmail(), request.ownerName(), request.ownerPassword());
        }

        return toResponse(org, owner);
    }

    @Transactional
    public OrganizationResponse update(UUID id, OrganizationRequest request) {
        Organization org = findActive(id);

        if (!org.getSlug().equals(request.slug())
                && organizationRepository.findBySlug(request.slug()).isPresent()) {
            throw new ConflictException("An organization with slug '" + request.slug() + "' already exists");
        }

        org.setName(request.name());
        org.setSlug(request.slug());
        if (request.vertical() != null) org.setVertical(request.vertical());
        if (request.timezone() != null) org.setTimezone(request.timezone());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        if (request.logoData() != null) org.setLogoData(request.logoData());
        if (request.primaryColor() != null) org.setPrimaryColor(request.primaryColor());

        return toResponse(organizationRepository.save(org), findOwner(id));
    }

    @Transactional
    public OrganizationResponse updateStatus(UUID id, String newStatus) {
        List<String> allowed = List.of("trial", "active", "suspended", "cancelled");
        if (!allowed.contains(newStatus)) {
            throw new BadRequestException("Status must be one of: " + allowed);
        }
        Organization org = findActive(id);
        org.setStatus(newStatus);
        return toResponse(organizationRepository.save(org), findOwner(id));
    }

    /**
     * Assigns organization ownership by email.
     * Reuses an existing user when the email is already registered; otherwise creates a new account.
     * Previous owners are demoted to Organization Admin (not disabled).
     */
    @Transactional
    public OrganizationResponse setOwner(UUID orgId, SetOwnerRequest request) {
        Organization org = findActive(orgId);
        User owner = assignOwnerUser(orgId, request.ownerEmail(), request.ownerName(), request.ownerPassword());
        return toResponse(org, owner);
    }

    /**
     * Promotes an existing active member to organization owner.
     * Previous owners are demoted to Organization Admin.
     */
    @Transactional
    public OrganizationResponse promoteOwner(UUID orgId, UUID userId) {
        Organization org = findActive(orgId);
        Role ownerRole = requireRole("organization_owner");

        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new NotFoundException("This person is not a member of the organization"));

        if (!"active".equals(membership.getStatus())) {
            throw new BadRequestException("Only active members can be promoted to owner");
        }

        if (membership.getRoleId().equals(ownerRole.getId())) {
            User alreadyOwner = userRepository.findById(userId).orElseThrow();
            return toResponse(org, alreadyOwner);
        }

        demotePreviousOwners(orgId, userId);
        membership.setRoleId(ownerRole.getId());
        membership.setStatus("active");
        membershipRepository.save(membership);

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(org, owner);
    }

    @Transactional
    public void delete(UUID id) {
        Organization org = findActive(id);
        org.setDeletedAt(Instant.now());
        organizationRepository.save(org);
    }

    // ── ownership helpers ─────────────────────────────────────────────────────

    /**
     * Ensures {@code email} holds the organization_owner role for {@code orgId}.
     * Creates the user when needed; links existing users without requiring a password.
     */
    private User assignOwnerUser(UUID orgId, String ownerEmail, String ownerName, String ownerPassword) {
        String email = ownerEmail.trim().toLowerCase(Locale.ROOT);
        Role ownerRole = requireRole("organization_owner");

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user != null) {
            if (ownerName != null && !ownerName.isBlank()) {
                user.setFullName(ownerName.trim());
                userRepository.save(user);
            }

            OrganizationMembership membership = membershipRepository
                    .findByOrganizationIdAndUserId(orgId, user.getId())
                    .orElse(null);

            if (membership != null && membership.getRoleId().equals(ownerRole.getId())
                    && "active".equals(membership.getStatus())) {
                return user;
            }

            demotePreviousOwners(orgId, user.getId());

            if (membership != null) {
                membership.setRoleId(ownerRole.getId());
                membership.setStatus("active");
                membershipRepository.save(membership);
            } else {
                OrganizationMembership created = new OrganizationMembership();
                created.setOrganizationId(orgId);
                created.setUserId(user.getId());
                created.setRoleId(ownerRole.getId());
                created.setStatus("active");
                membershipRepository.save(created);
            }
            return user;
        }

        if (ownerName == null || ownerName.isBlank()) {
            throw new BadRequestException("Owner name is required when creating a new owner account");
        }
        if (ownerPassword == null || ownerPassword.isBlank()) {
            throw new BadRequestException("Password is required when creating a new owner account");
        }

        demotePreviousOwners(orgId, null);

        User created = new User();
        created.setFullName(ownerName.trim());
        created.setEmail(email);
        created.setPasswordHash(passwordEncoder.encode(ownerPassword));
        created.setStatus("active");
        created.setPlatformAdmin(false);
        created = userRepository.save(created);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganizationId(orgId);
        membership.setUserId(created.getId());
        membership.setRoleId(ownerRole.getId());
        membership.setStatus("active");
        membershipRepository.save(membership);

        return created;
    }

    /** Demotes every other organization_owner in this org to organization_admin. */
    private void demotePreviousOwners(UUID orgId, UUID newOwnerUserId) {
        Role ownerRole = requireRole("organization_owner");
        Role adminRole = requireRole("organization_admin");

        membershipRepository.findByOrganizationId(orgId).stream()
                .filter(m -> m.getRoleId().equals(ownerRole.getId()))
                .filter(m -> newOwnerUserId == null || !m.getUserId().equals(newOwnerUserId))
                .forEach(m -> {
                    m.setRoleId(adminRole.getId());
                    m.setStatus("active");
                    membershipRepository.save(m);
                    invalidateSession(m.getUserId());
                });
    }

    private void invalidateSession(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setActiveSessionToken(null);
            userRepository.save(u);
        });
    }

    private Role requireRole(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(code + " role not found — run DataSeeder first"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Organization findActive(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        if (org.getDeletedAt() != null) {
            throw new NotFoundException("Organization not found");
        }
        return org;
    }

    private User findOwner(UUID orgId) {
        return roleRepository.findByCode("organization_owner").map(role ->
                membershipRepository.findByOrganizationId(orgId).stream()
                        .filter(m -> m.getRoleId().equals(role.getId()) && "active".equals(m.getStatus()))
                        .findFirst()
                        .flatMap(m -> userRepository.findById(m.getUserId()))
                        .orElse(null)
        ).orElse(null);
    }

    private OrganizationResponse toResponse(Organization o, User owner) {
        return new OrganizationResponse(
                o.getId(), o.getName(), o.getSlug(), o.getVertical(),
                o.getStatus(), o.getTimezone(), o.getLogoUrl(), o.getLogoData(),
                o.getPrimaryColor(), o.getCreatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}
