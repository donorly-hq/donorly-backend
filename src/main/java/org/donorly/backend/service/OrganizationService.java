package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.OrganizationRequest;
import org.donorly.backend.dto.OrganizationResponse;
import org.donorly.backend.dto.SetOwnerRequest;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
            if (request.ownerPassword() == null || request.ownerPassword().isBlank()) {
                throw new BadRequestException("Owner password is required when creating with an owner account");
            }
            if (userRepository.existsByEmailIgnoreCase(request.ownerEmail())) {
                throw new ConflictException("A user with email '" + request.ownerEmail() + "' already exists");
            }
        }

        // ── create org ────────────────────────────────────────────────────────
        Organization org = new Organization();
        org.setName(request.name());
        org.setSlug(request.slug());
        org.setVertical(request.vertical() != null ? request.vertical() : "nonprofit");
        org.setTimezone(request.timezone() != null ? request.timezone() : "America/Chicago");
        org.setLogoUrl(request.logoUrl());
        org.setPrimaryColor(request.primaryColor());
        org.setStatus("trial");
        org = organizationRepository.save(org);

        // ── org settings ──────────────────────────────────────────────────────
        if (!settingsRepository.existsById(org.getId())) {
            OrganizationSettings settings = new OrganizationSettings();
            settings.setOrganizationId(org.getId());
            settingsRepository.save(settings);
        }

        // ── owner user + membership ───────────────────────────────────────────
        User owner = null;
        if (hasOwner) {
            owner = new User();
            owner.setFullName(request.ownerName());
            owner.setEmail(request.ownerEmail().toLowerCase());
            owner.setPasswordHash(passwordEncoder.encode(request.ownerPassword()));
            owner.setStatus("active");
            owner.setPlatformAdmin(false);
            owner = userRepository.save(owner);

            Role ownerRole = roleRepository.findByCode("organization_owner")
                    .orElseThrow(() -> new IllegalStateException("organization_owner role not found — run DataSeeder first"));

            OrganizationMembership membership = new OrganizationMembership();
            membership.setOrganizationId(org.getId());
            membership.setUserId(owner.getId());
            membership.setRoleId(ownerRole.getId());
            membership.setStatus("active");
            membershipRepository.save(membership);
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
     * Creates (or replaces) the organization_owner account for an existing org.
     * If an owner already exists their account is deactivated and a new one is created.
     */
    @Transactional
    public OrganizationResponse setOwner(UUID orgId, SetOwnerRequest request) {
        Organization org = findActive(orgId);

        if (userRepository.existsByEmailIgnoreCase(request.ownerEmail())) {
            // user already exists — check if they are already the owner of this org
            User existing = userRepository.findByEmailIgnoreCase(request.ownerEmail()).orElseThrow();
            boolean alreadyMember = membershipRepository
                    .findByOrganizationIdAndUserId(orgId, existing.getId()).isPresent();
            if (!alreadyMember) {
                throw new ConflictException("A user with email '" + request.ownerEmail() + "' already exists in the system");
            }
        }

        // deactivate any existing organization_owner membership for this org
        Role ownerRole = roleRepository.findByCode("organization_owner")
                .orElseThrow(() -> new IllegalStateException("organization_owner role not found"));
        membershipRepository.findByOrganizationId(orgId).stream()
                .filter(m -> m.getRoleId().equals(ownerRole.getId()))
                .forEach(m -> {
                    m.setStatus("disabled");
                    membershipRepository.save(m);
                });

        // create new owner user
        User owner = new User();
        owner.setFullName(request.ownerName());
        owner.setEmail(request.ownerEmail().toLowerCase());
        owner.setPasswordHash(passwordEncoder.encode(request.ownerPassword()));
        owner.setStatus("active");
        owner.setPlatformAdmin(false);
        owner = userRepository.save(owner);

        // create membership
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganizationId(orgId);
        membership.setUserId(owner.getId());
        membership.setRoleId(ownerRole.getId());
        membership.setStatus("active");
        membershipRepository.save(membership);

        return toResponse(org, owner);
    }

    @Transactional
    public void delete(UUID id) {
        Organization org = findActive(id);
        org.setDeletedAt(Instant.now());
        organizationRepository.save(org);
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

    /** Returns the organization_owner member's User, or null if none. */
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
                o.getStatus(), o.getTimezone(), o.getLogoUrl(),
                o.getPrimaryColor(), o.getCreatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}
