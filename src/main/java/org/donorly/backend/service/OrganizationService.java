package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.OrgMemberSummary;
import org.donorly.backend.dto.OrganizationRequest;
import org.donorly.backend.dto.OrganizationResponse;
import org.donorly.backend.dto.OrganizationSummary;
import org.donorly.backend.dto.SetOwnerRequest;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Duration ACCOUNT_SETUP_TTL = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository sessionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrgLogoService orgLogoService;
    private final EmailService emailService;

    @Value("${donorly.mail.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    public List<OrganizationSummary> listAll() {
        return organizationRepository.findAll()
                .stream()
                .filter(o -> o.getDeletedAt() == null)
                .map(o -> toSummary(o, findOwner(o.getId())))
                .toList();
    }

    public OrganizationResponse getById(UUID id) {
        Organization org = findActive(id);
        return toResponse(org, findOwner(id));
    }

    /** Active members of an org — for platform-admin owner transfer UI. */
    public List<OrgMemberSummary> listMembers(UUID orgId) {
        findActive(orgId);
        List<OrganizationMembership> memberships = membershipRepository.findByOrganizationId(orgId).stream()
                .filter(m -> MembershipStatus.ACTIVE.matches(m.getStatus()))
                .toList();

        // Batch-load users and roles (2 queries) instead of 2 findById calls per member.
        var usersById = new java.util.HashMap<UUID, User>();
        userRepository.findAllById(memberships.stream().map(OrganizationMembership::getUserId).toList())
                .forEach(u -> usersById.put(u.getId(), u));
        var rolesById = new java.util.HashMap<UUID, Role>();
        roleRepository.findAllById(memberships.stream().map(OrganizationMembership::getRoleId).toList())
                .forEach(r -> rolesById.put(r.getId(), r));

        return memberships.stream()
                .map(m -> {
                    User user = usersById.get(m.getUserId());
                    if (user == null) return null;
                    Role role = rolesById.get(m.getRoleId());
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
        if (hasOwner && (request.ownerName() == null || request.ownerName().isBlank())) {
            throw new BadRequestException("Owner name is required when creating with an owner account");
        }

        Organization org = new Organization();
        org.setName(request.name());
        org.setSlug(request.slug());
        org.setVertical(request.vertical() != null ? request.vertical() : "nonprofit");
        org.setTimezone(request.timezone() != null ? request.timezone() : "America/Chicago");
        org.setLogoUrl(request.logoUrl());
        org.setPrimaryColor(request.primaryColor());
        org.setStatus("trial");
        org = organizationRepository.save(org);

        applyLogoUpload(org.getId(), request.logoData());

        if (!settingsRepository.existsById(org.getId())) {
            OrganizationSettings settings = new OrganizationSettings();
            settings.setOrganizationId(org.getId());
            settingsRepository.save(settings);
        }

        User owner = null;
        if (hasOwner) {
            owner = assignOwnerUser(org.getId(), request.ownerEmail(), request.ownerName());
        }

        org = organizationRepository.findById(org.getId()).orElseThrow();
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
        org = organizationRepository.save(org);

        applyLogoUpload(id, request.logoData());

        return toResponse(organizationRepository.findById(id).orElseThrow(), findOwner(id));
    }

    private void applyLogoUpload(UUID orgId, String logoData) {
        if (logoData != null && !logoData.isBlank()) {
            orgLogoService.saveFromDataUrl(orgId, logoData);
        }
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
        User owner = assignOwnerUser(orgId, request.ownerEmail(), request.ownerName());
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

        if (!MembershipStatus.ACTIVE.matches(membership.getStatus())) {
            throw new BadRequestException("Only active members can be promoted to owner");
        }

        if (membership.getRoleId().equals(ownerRole.getId())) {
            User alreadyOwner = userRepository.findById(userId).orElseThrow();
            return toResponse(org, alreadyOwner);
        }

        demotePreviousOwners(orgId, userId);
        membership.setRoleId(ownerRole.getId());
        membership.setStatus(MembershipStatus.ACTIVE.value());
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
     * Links existing users directly; brand-new accounts are created without a usable
     * password and receive a registration email with a set-password link.
     */
    private User assignOwnerUser(UUID orgId, String ownerEmail, String ownerName) {
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

            boolean alreadyOwner = membership != null && membership.getRoleId().equals(ownerRole.getId())
                    && MembershipStatus.ACTIVE.matches(membership.getStatus());

            if (!alreadyOwner) {
                demotePreviousOwners(orgId, user.getId());

                if (membership != null) {
                    membership.setRoleId(ownerRole.getId());
                    membership.setStatus(MembershipStatus.ACTIVE.value());
                    membershipRepository.save(membership);
                } else {
                    OrganizationMembership created = new OrganizationMembership();
                    created.setOrganizationId(orgId);
                    created.setUserId(user.getId());
                    created.setRoleId(ownerRole.getId());
                    created.setStatus(MembershipStatus.ACTIVE.value());
                    membershipRepository.save(created);
                }
            }

            // Accounts created by an admin that were never signed into don't have a
            // usable password from the owner's perspective — send the set-password
            // email so they can complete registration. Runs even when the membership
            // is unchanged, so re-assigning the owner re-sends the email.
            if (user.getLastLoginAt() == null) {
                sendAccountSetupEmail(user, orgId);
            }
            return user;
        }

        if (ownerName == null || ownerName.isBlank()) {
            throw new BadRequestException("Owner name is required when creating a new owner account");
        }

        demotePreviousOwners(orgId, null);

        User created = new User();
        created.setFullName(ownerName.trim());
        created.setEmail(email);
        // Unknowable placeholder — the real password is chosen by the owner via the
        // emailed set-password link (or later through the forgot-password flow).
        created.setPasswordHash(passwordEncoder.encode(randomSecret()));
        created.setStatus("active");
        created.setPlatformAdmin(false);
        created = userRepository.save(created);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganizationId(orgId);
        membership.setUserId(created.getId());
        membership.setRoleId(ownerRole.getId());
        membership.setStatus(MembershipStatus.ACTIVE.value());
        membershipRepository.save(membership);

        sendAccountSetupEmail(created, orgId);

        return created;
    }

    /** Emails a new account a single-use link to choose their password (registration). */
    private void sendAccountSetupEmail(User user, UUID orgId) {
        authTokenRepository.deleteByUserIdAndPurpose(user.getId(), AuthToken.PURPOSE_ACCOUNT_SETUP);

        AuthToken token = new AuthToken();
        token.setUserId(user.getId());
        token.setToken(randomSecret());
        token.setPurpose(AuthToken.PURPOSE_ACCOUNT_SETUP);
        token.setExpiresAt(Instant.now().plus(ACCOUNT_SETUP_TTL));
        authTokenRepository.save(token);

        String orgName = organizationRepository.findById(orgId)
                .map(Organization::getName).orElse("Donorly");
        String link = appBaseUrl + "/reset-password/" + token.getToken();
        emailService.sendHtml(user.getEmail(), "Welcome to Donorly — set your password", """
                <!DOCTYPE html>
                <html>
                <body style="font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:24px">
                  <h2 style="color:#2563eb">Welcome to Donorly</h2>
                  <p>Hi %s,</p>
                  <p>An account has been created for you as the owner of <strong>%s</strong>.
                     Set your password to complete your registration and sign in.</p>
                  <p style="margin:32px 0">
                    <a href="%s"
                       style="background:#2563eb;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:600">
                      Set My Password
                    </a>
                  </p>
                  <p style="font-size:13px;color:#6b7280">This link expires in 7 days. If it expires, use
                     "Forgot password" on the sign-in page to get a new one.</p>
                </body>
                </html>
                """.formatted(user.getFullName(), orgName, link));
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
                    m.setStatus(MembershipStatus.ACTIVE.value());
                    membershipRepository.save(m);
                    invalidateSession(m.getUserId());
                });
    }

    private void invalidateSession(UUID userId) {
        // Role changed — force fresh logins so new permissions take effect
        sessionRepository.deleteByUserId(userId);
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

    private OrganizationSummary toSummary(Organization o, User owner) {
        return new OrganizationSummary(
                o.getId(), o.getName(), o.getSlug(), o.getVertical(),
                o.getStatus(), o.getTimezone(), o.getPrimaryColor(),
                orgLogoService.hasLogo(o), o.getCreatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }

    private OrganizationResponse toResponse(Organization o, User owner) {
        return new OrganizationResponse(
                o.getId(), o.getName(), o.getSlug(), o.getVertical(),
                o.getStatus(), o.getTimezone(), o.getLogoUrl(),
                orgLogoService.hasLogo(o), o.getPrimaryColor(), o.getCreatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getEmail() : null
        );
    }
}
