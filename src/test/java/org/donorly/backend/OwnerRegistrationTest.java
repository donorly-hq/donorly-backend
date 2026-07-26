package org.donorly.backend;

import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.dto.SetOwnerRequest;
import org.donorly.backend.model.AuthToken;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.Role;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.AuthTokenRepository;
import org.donorly.backend.service.AuthService;
import org.donorly.backend.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Owner assignment no longer takes a password: brand-new accounts must receive an
 * account_setup token (delivered by email) and complete registration by choosing
 * their password through the shared reset-password endpoint.
 */
class OwnerRegistrationTest extends IntegrationTestBase {

    @Autowired private OrganizationService organizationService;
    @Autowired private AuthService authService;
    @Autowired private AuthTokenRepository authTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void newOwnerGetsSetupTokenAndCompletesRegistration() {
        Organization org = createOrg("Setup Flow Org");
        String email = "owner-" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        organizationService.setOwner(org.getId(), new SetOwnerRequest("New Owner", email));

        User owner = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals("active", owner.getStatus());
        assertNotNull(owner.getPasswordHash(), "placeholder hash must exist so forgot-password works");

        Role ownerRole = roleRepository.findByCode("organization_owner").orElseThrow();
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserId(org.getId(), owner.getId()).orElseThrow();
        assertEquals(ownerRole.getId(), membership.getRoleId());

        AuthToken setupToken = authTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(owner.getId())
                        && AuthToken.PURPOSE_ACCOUNT_SETUP.equals(t.getPurpose()))
                .findFirst().orElseThrow();

        // The emailed link completes registration through the reset-password endpoint.
        authService.resetPassword(setupToken.getToken(), "brand-new-password-123", "10.1.1.1");

        User registered = userRepository.findById(owner.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("brand-new-password-123", registered.getPasswordHash()));

        // Single-use: replaying the same token must fail.
        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(setupToken.getToken(), "another-password-456", "10.1.1.2"));
    }

    @Test
    void existingActiveUserLinkedAsOwnerGetsNoSetupToken() {
        Organization org = createOrg("Existing Owner Org");
        TestActor existing = createActor(createOrg("Home Org"), "organization_admin");
        // Simulate a real user who has signed in before — they know their password.
        User user = existing.user();
        user.setLastLoginAt(java.time.Instant.now());
        userRepository.save(user);

        organizationService.setOwner(org.getId(),
                new SetOwnerRequest("Renamed Owner", user.getEmail()));

        List<AuthToken> setupTokens = authTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId())
                        && AuthToken.PURPOSE_ACCOUNT_SETUP.equals(t.getPurpose()))
                .toList();
        assertTrue(setupTokens.isEmpty(), "accounts that have signed in keep their password; no setup email");
    }

    @Test
    void existingUserWhoNeverLoggedInStillGetsSetupToken() {
        Organization org = createOrg("Dormant Owner Org");
        // An account created by an admin (e.g. via the old flow) that was never used.
        TestActor dormant = createActor(createOrg("Dormant Home Org"), "organization_admin");

        organizationService.setOwner(org.getId(),
                new SetOwnerRequest("Dormant Owner", dormant.user().getEmail()));

        List<AuthToken> setupTokens = authTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(dormant.user().getId())
                        && AuthToken.PURPOSE_ACCOUNT_SETUP.equals(t.getPurpose()))
                .toList();
        assertEquals(1, setupTokens.size(), "never-logged-in accounts need the set-password email");

        // Re-assigning the same owner re-issues the token (email resend path).
        organizationService.setOwner(org.getId(),
                new SetOwnerRequest("Dormant Owner", dormant.user().getEmail()));
        long tokenCount = authTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(dormant.user().getId())
                        && AuthToken.PURPOSE_ACCOUNT_SETUP.equals(t.getPurpose()))
                .count();
        assertEquals(1, tokenCount, "old setup tokens are replaced, not accumulated");
    }
}
