package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.common.Permissions;
import org.donorly.backend.security.SecurityUtils;
import org.donorly.backend.dto.*;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeamService {

    private static final long INVITE_TTL_DAYS = 7;
    private static final Set<String> NON_ASSIGNABLE_ROLES = Set.of("platform_super_admin", "donor");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailService emailService;

    @Value("${donorly.mail.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    private final SecureRandom random = new SecureRandom();

    // ---- Members --------------------------------------------------------

    public List<TeamMemberResponse> listMembers() {
        UUID orgId = TenantContext.requireOrganizationId();
        Map<UUID, Role> rolesById = new HashMap<>();
        roleRepository.findAll().forEach(r -> rolesById.put(r.getId(), r));

        List<TeamMemberResponse> result = new ArrayList<>();
        for (OrganizationMembership m : membershipRepository.findByOrganizationId(orgId)) {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            Role role = rolesById.get(m.getRoleId());
            result.add(new TeamMemberResponse(
                    m.getId(),
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    role != null ? role.getCode() : null,
                    role != null ? role.getName() : null,
                    m.getStatus(),
                    user.getLastLoginAt()
            ));
        }
        result.sort(Comparator.comparing(TeamMemberResponse::fullName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /** Active members only — used to populate donor-assignment pickers for donors.write holders. */
    public List<TeamMemberResponse> listActiveMembers() {
        List<TeamMemberResponse> members = new ArrayList<>();
        for (TeamMemberResponse m : listMembers()) {
            if ("active".equals(m.status())) {
                members.add(m);
            }
        }
        return members;
    }

    public List<RoleResponse> listAssignableRoles() {
        List<RoleResponse> roles = new ArrayList<>();
        for (Role r : roleRepository.findAll()) {
            if ("organization".equals(r.getScope()) && !NON_ASSIGNABLE_ROLES.contains(r.getCode())) {
                roles.add(new RoleResponse(r.getCode(), r.getName()));
            }
        }
        roles.sort(Comparator.comparing(RoleResponse::name, String.CASE_INSENSITIVE_ORDER));
        return roles;
    }

    @Transactional
    public TeamMemberResponse updateMember(UUID userId, MemberUpdateRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        if (request.roleCode() != null && !request.roleCode().isBlank()) {
            Role role = requireAssignableRole(request.roleCode());
            membership.setRoleId(role.getId());
        }
        if (request.status() != null && !request.status().isBlank()) {
            if (!Set.of("active", "disabled", "invited").contains(request.status())) {
                throw new BadRequestException("Invalid status");
            }
            membership.setStatus(request.status());
        }
        membershipRepository.save(membership);
        auditService.record("team.member.update", "membership", membership.getId());

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Role role = roleRepository.findById(membership.getRoleId()).orElse(null);
        return new TeamMemberResponse(membership.getId(), user.getId(), user.getFullName(), user.getEmail(),
                role != null ? role.getCode() : null, role != null ? role.getName() : null,
                membership.getStatus(), user.getLastLoginAt());
    }

    // ---- Invitations ----------------------------------------------------

    public List<InvitationResponse> listPendingInvitations() {
        UUID orgId = TenantContext.requireOrganizationId();
        Map<UUID, Role> rolesById = new HashMap<>();
        roleRepository.findAll().forEach(r -> rolesById.put(r.getId(), r));
        List<InvitationResponse> result = new ArrayList<>();
        for (Invitation inv : invitationRepository.findByOrganizationIdAndStatus(orgId, "pending")) {
            Role role = rolesById.get(inv.getRoleId());
            result.add(new InvitationResponse(inv.getId(), inv.getEmail(),
                    role != null ? role.getCode() : null, role != null ? role.getName() : null,
                    inv.getStatus(), inv.getExpiresAt(), inv.getCreatedAt(), null));
        }
        return result;
    }

    @Transactional
    public InvitationResponse invite(InvitationRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        Role role = requireAssignableRole(request.roleCode());

        // Campaign managers (team.invite only, not users.manage) may only invite ambassadors
        boolean fullAdmin = SecurityUtils.hasAuthority(Permissions.USERS_MANAGE);
        if (!fullAdmin && !"ambassador".equals(role.getCode())) {
            throw new BadRequestException("You can only invite Ambassadors");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (membershipRepository.findByOrganizationIdAndUserId(orgId, existing.getId()).isPresent()) {
                throw new ConflictException("This person is already a member of the organization");
            }
        });

        invitationRepository.findByOrganizationIdAndEmailIgnoreCaseAndStatus(orgId, email, "pending")
                .ifPresent(inv -> {
                    inv.setStatus("revoked");
                    invitationRepository.save(inv);
                });

        String rawToken = generateToken();
        Invitation invitation = new Invitation();
        invitation.setOrganizationId(orgId);
        invitation.setEmail(email);
        invitation.setRoleId(role.getId());
        invitation.setTokenHash(hashToken(rawToken));
        invitation.setStatus("pending");
        invitation.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
        Invitation saved = invitationRepository.save(invitation);
        auditService.record("team.invite.create", "invitation", saved.getId());

        // Send invitation email asynchronously
        String orgName = organizationRepository.findById(orgId).map(o -> o.getName()).orElse("Donorly");
        String inviteLink = appBaseUrl + "/invite/" + rawToken;
        emailService.sendHtml(
                email,
                "You're invited to join " + orgName + " on Donorly",
                buildInviteEmail(orgName, role.getName(), inviteLink)
        );

        return new InvitationResponse(saved.getId(), saved.getEmail(), role.getCode(), role.getName(),
                saved.getStatus(), saved.getExpiresAt(), saved.getCreatedAt(), rawToken);
    }

    @Transactional
    public void revokeInvitation(UUID id) {
        UUID orgId = TenantContext.requireOrganizationId();
        Invitation invitation = invitationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        invitation.setStatus("revoked");
        invitationRepository.save(invitation);
        auditService.record("team.invite.revoke", "invitation", id);
    }

    // ---- Public accept flow --------------------------------------------

    public InvitationInfoResponse getInvitationInfo(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(hashToken(rawToken)).orElse(null);
        if (invitation == null) {
            return new InvitationInfoResponse(null, null, null, false, false);
        }
        boolean valid = "pending".equals(invitation.getStatus())
                && invitation.getExpiresAt().isAfter(Instant.now());
        Organization org = organizationRepository.findById(invitation.getOrganizationId()).orElse(null);
        Role role = roleRepository.findById(invitation.getRoleId()).orElse(null);
        boolean existingUser = userRepository.findByEmailIgnoreCase(invitation.getEmail()).isPresent();
        return new InvitationInfoResponse(
                org != null ? org.getName() : null,
                invitation.getEmail(),
                role != null ? role.getName() : null,
                valid,
                existingUser
        );
    }

    @Transactional
    public void acceptInvitation(AcceptInviteRequest request) {
        Invitation invitation = invitationRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid invitation"));
        if (!"pending".equals(invitation.getStatus())) {
            throw new BadRequestException("This invitation is no longer valid");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("expired");
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired");
        }

        User user = userRepository.findByEmailIgnoreCase(invitation.getEmail()).orElse(null);
        if (user == null) {
            if (request.password() == null || request.password().length() < 8) {
                throw new BadRequestException("A password of at least 8 characters is required");
            }
            if (request.fullName() == null || request.fullName().isBlank()) {
                throw new BadRequestException("Your name is required");
            }
            user = new User();
            user.setEmail(invitation.getEmail());
            user.setFullName(request.fullName().trim());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setStatus("active");
            user = userRepository.save(user);
        }

        UUID userId = user.getId();
        if (membershipRepository.findByOrganizationIdAndUserId(invitation.getOrganizationId(), userId).isEmpty()) {
            OrganizationMembership membership = new OrganizationMembership();
            membership.setOrganizationId(invitation.getOrganizationId());
            membership.setUserId(userId);
            membership.setRoleId(invitation.getRoleId());
            membership.setStatus("active");
            membershipRepository.save(membership);
        }

        invitation.setStatus("accepted");
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
    }

    // ---- helpers --------------------------------------------------------

    private Role requireAssignableRole(String code) {
        if (NON_ASSIGNABLE_ROLES.contains(code)) {
            throw new BadRequestException("That role cannot be assigned");
        }
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Unknown role"));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private String buildInviteEmail(String orgName, String roleName, String inviteLink) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:24px">
                  <h2 style="color:#2563eb">You've been invited to Donorly</h2>
                  <p>You have been invited to join <strong>%s</strong> as <strong>%s</strong>.</p>
                  <p style="margin:32px 0">
                    <a href="%s"
                       style="background:#2563eb;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none;font-weight:600">
                      Accept Invitation
                    </a>
                  </p>
                  <p style="font-size:13px;color:#6b7280">This link expires in 7 days. If you did not expect this email, you can safely ignore it.</p>
                </body>
                </html>
                """.formatted(orgName, roleName, inviteLink);
    }
}
