package org.donorly.backend.config;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.Permissions;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Seeds baseline permissions, system roles, a default organization and a platform
 * super admin on an empty database. Controlled by {@code donorly.bootstrap.*}.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${donorly.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;
    @Value("${donorly.bootstrap.super-admin-email:admin@donorly.org}")
    private String superAdminEmail;
    @Value("${donorly.bootstrap.super-admin-password:ChangeMe123!}")
    private String superAdminPassword;
    @Value("${donorly.bootstrap.default-org-name:Jamia Masjid Chicago}")
    private String defaultOrgName;
    @Value("${donorly.bootstrap.default-org-slug:jamia-masjid-chicago}")
    private String defaultOrgSlug;

    @Override
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }
        seedPermissions();
        seedRoles();
        seedDefaultOrganizationAndAdmin();
        seedDemoUsers();
    }

    private void seedPermissions() {
        List<String> codes = List.of(
                Permissions.PLATFORM_ORGANIZATIONS_MANAGE,
                Permissions.ORG_SETTINGS_MANAGE,
                Permissions.USERS_MANAGE,
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL, Permissions.DONORS_WRITE,
                Permissions.DONORS_ASSIGN, Permissions.DONORS_DELETE, Permissions.DONORS_EXPORT,
                Permissions.CAMPAIGNS_READ, Permissions.CAMPAIGNS_MANAGE,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.FOLLOWUPS_READ, Permissions.FOLLOWUPS_WRITE,
                Permissions.REPORTS_VIEW,
                Permissions.PAYMENTS_MANAGE, Permissions.RECEIPTS_ISSUE,
                Permissions.EVENTS_READ, Permissions.EVENTS_MANAGE, Permissions.EVENTS_CHECKIN,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE, Permissions.VOLUNTEERS_MANAGE,
                Permissions.TOWNHALLS_READ, Permissions.TOWNHALLS_MANAGE,
                Permissions.COMMUNICATIONS_READ, Permissions.COMMUNICATIONS_MANAGE, Permissions.COMMUNICATIONS_SEND,
                Permissions.AI_USE, Permissions.AI_ADMIN,
                Permissions.TEAM_INVITE
        );
        for (String code : codes) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                Permission p = new Permission();
                p.setCode(code);
                p.setDescription(code);
                permissionRepository.save(p);
            }
        }
    }

    private void seedRoles() {
        upsertRole("platform_super_admin", "Platform Super Admin", "platform",
                Set.of(Permissions.PLATFORM_ORGANIZATIONS_MANAGE));

        Set<String> ownerPerms = new HashSet<>(Arrays.asList(
                Permissions.ORG_SETTINGS_MANAGE, Permissions.USERS_MANAGE,
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL, Permissions.DONORS_WRITE,
                Permissions.DONORS_ASSIGN, Permissions.DONORS_DELETE, Permissions.DONORS_EXPORT,
                Permissions.CAMPAIGNS_READ, Permissions.CAMPAIGNS_MANAGE,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.FOLLOWUPS_READ, Permissions.FOLLOWUPS_WRITE,
                Permissions.REPORTS_VIEW, Permissions.PAYMENTS_MANAGE, Permissions.RECEIPTS_ISSUE,
                Permissions.EVENTS_READ, Permissions.EVENTS_MANAGE, Permissions.EVENTS_CHECKIN,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE, Permissions.VOLUNTEERS_MANAGE,
                Permissions.TOWNHALLS_READ, Permissions.TOWNHALLS_MANAGE,
                Permissions.COMMUNICATIONS_READ, Permissions.COMMUNICATIONS_MANAGE, Permissions.COMMUNICATIONS_SEND,
                Permissions.AI_USE, Permissions.AI_ADMIN));
        upsertRole("organization_owner", "Organization Owner", "organization", ownerPerms);

        Set<String> adminPerms = new HashSet<>(Arrays.asList(
                Permissions.ORG_SETTINGS_MANAGE, Permissions.USERS_MANAGE,
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL, Permissions.DONORS_WRITE,
                Permissions.DONORS_ASSIGN, Permissions.DONORS_DELETE, Permissions.DONORS_EXPORT,
                Permissions.CAMPAIGNS_READ, Permissions.CAMPAIGNS_MANAGE,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.FOLLOWUPS_READ, Permissions.FOLLOWUPS_WRITE,
                Permissions.REPORTS_VIEW,
                Permissions.EVENTS_READ, Permissions.EVENTS_MANAGE, Permissions.EVENTS_CHECKIN,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE, Permissions.VOLUNTEERS_MANAGE,
                Permissions.TOWNHALLS_READ, Permissions.TOWNHALLS_MANAGE,
                Permissions.COMMUNICATIONS_READ, Permissions.COMMUNICATIONS_MANAGE, Permissions.COMMUNICATIONS_SEND,
                Permissions.AI_USE));
        upsertRole("organization_admin", "Organization Admin", "organization", adminPerms);

        upsertRole("campaign_manager", "Campaign Manager", "organization", Set.of(
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL, Permissions.DONORS_WRITE,
                Permissions.DONORS_ASSIGN, Permissions.DONORS_DELETE,
                Permissions.CAMPAIGNS_READ, Permissions.CAMPAIGNS_MANAGE,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.FOLLOWUPS_READ, Permissions.FOLLOWUPS_WRITE,
                Permissions.REPORTS_VIEW,
                Permissions.EVENTS_READ, Permissions.EVENTS_MANAGE, Permissions.EVENTS_CHECKIN,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE, Permissions.VOLUNTEERS_MANAGE,
                Permissions.TOWNHALLS_READ, Permissions.TOWNHALLS_MANAGE,
                Permissions.COMMUNICATIONS_READ, Permissions.COMMUNICATIONS_MANAGE, Permissions.COMMUNICATIONS_SEND,
                Permissions.TEAM_INVITE));

        upsertRole("finance_user", "Finance User", "organization", Set.of(
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.PAYMENTS_MANAGE, Permissions.RECEIPTS_ISSUE, Permissions.REPORTS_VIEW,
                Permissions.EVENTS_READ,
                Permissions.COMMUNICATIONS_READ));

        upsertRole("ambassador", "Ambassador", "organization", Set.of(
                Permissions.DONORS_READ, Permissions.DONORS_READ_ALL, Permissions.DONORS_WRITE,
                Permissions.CAMPAIGNS_READ,
                Permissions.PLEDGES_READ, Permissions.PLEDGES_WRITE,
                Permissions.FOLLOWUPS_READ, Permissions.FOLLOWUPS_WRITE,
                Permissions.EVENTS_READ,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE,
                Permissions.TOWNHALLS_READ, Permissions.TOWNHALLS_MANAGE,
                Permissions.COMMUNICATIONS_READ, Permissions.COMMUNICATIONS_SEND));

        upsertRole("volunteer", "Volunteer", "organization", Set.of(
                Permissions.DONORS_READ, Permissions.PLEDGES_WRITE,
                Permissions.EVENTS_READ, Permissions.EVENTS_CHECKIN,
                Permissions.VOLUNTEERS_READ, Permissions.VOLUNTEERS_WRITE));

        upsertRole("donor", "Donor", "organization", Collections.emptySet());
    }

    private void upsertRole(String code, String name, String scope, Set<String> permissionCodes) {
        Role role = roleRepository.findByCode(code).orElseGet(Role::new);
        role.setCode(code);
        role.setName(name);
        role.setScope(scope);
        role.setSystem(true);
        Set<Permission> perms = new HashSet<>();
        for (String pc : permissionCodes) {
            permissionRepository.findByCode(pc).ifPresent(perms::add);
        }
        role.setPermissions(perms);
        roleRepository.save(role);
    }

    private void seedDefaultOrganizationAndAdmin() {
        if (userRepository.existsByEmailIgnoreCase(superAdminEmail)) {
            return;
        }

        Organization org = organizationRepository.findBySlug(defaultOrgSlug).orElseGet(() -> {
            Organization o = new Organization();
            o.setName(defaultOrgName);
            o.setSlug(defaultOrgSlug);
            o.setVertical("mosque");
            o.setStatus("active");
            return organizationRepository.save(o);
        });

        if (!settingsRepository.existsById(org.getId())) {
            OrganizationSettings settings = new OrganizationSettings();
            settings.setOrganizationId(org.getId());
            settingsRepository.save(settings);
        }

        User admin = new User();
        admin.setEmail(superAdminEmail);
        admin.setFullName("Platform Super Admin");
        admin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        admin.setStatus("active");
        admin.setPlatformAdmin(true);
        admin = userRepository.save(admin);

        Role ownerRole = roleRepository.findByCode("organization_owner").orElseThrow();
        if (membershipRepository.findByOrganizationIdAndUserId(org.getId(), admin.getId()).isEmpty()) {
            OrganizationMembership membership = new OrganizationMembership();
            membership.setOrganizationId(org.getId());
            membership.setUserId(admin.getId());
            membership.setRoleId(ownerRole.getId());
            membership.setStatus("active");
            membershipRepository.save(membership);
        }
    }

    /**
     * Creates one demo account per role in the default organisation.
     * Accounts are only created when the email does not yet exist, so this is safe
     * to run on every startup.
     *
     * Credentials (all use password:  Demo1234!):
     *   owner@demo.donorly.org          — Organization Owner
     *   admin@demo.donorly.org          — Organization Admin
     *   campaign@demo.donorly.org       — Campaign Manager
     *   finance@demo.donorly.org        — Finance User
     *   ambassador@demo.donorly.org     — Ambassador
     *   volunteer@demo.donorly.org      — Volunteer
     */
    private void seedDemoUsers() {
        Organization org = organizationRepository.findBySlug(defaultOrgSlug).orElse(null);
        if (org == null) {
            return;
        }

        String demoPassword = "Demo1234!";

        List<String[]> demoAccounts = List.of(
                new String[]{"owner@demo.donorly.org",      "Demo Org Owner",        "organization_owner"},
                new String[]{"admin@demo.donorly.org",      "Demo Org Admin",        "organization_admin"},
                new String[]{"campaign@demo.donorly.org",   "Demo Campaign Manager", "campaign_manager"},
                new String[]{"finance@demo.donorly.org",    "Demo Finance User",     "finance_user"},
                new String[]{"ambassador@demo.donorly.org", "Demo Ambassador",       "ambassador"},
                new String[]{"volunteer@demo.donorly.org",  "Demo Volunteer",        "volunteer"}
        );

        for (String[] account : demoAccounts) {
            String email    = account[0];
            String fullName = account[1];
            String roleCode = account[2];

            if (userRepository.existsByEmailIgnoreCase(email)) {
                continue;
            }

            Role role = roleRepository.findByCode(roleCode).orElse(null);
            if (role == null) {
                continue;
            }

            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPasswordHash(passwordEncoder.encode(demoPassword));
            user.setStatus("active");
            user = userRepository.save(user);

            OrganizationMembership membership = new OrganizationMembership();
            membership.setOrganizationId(org.getId());
            membership.setUserId(user.getId());
            membership.setRoleId(role.getId());
            membership.setStatus("active");
            membershipRepository.save(membership);
        }
    }
}
