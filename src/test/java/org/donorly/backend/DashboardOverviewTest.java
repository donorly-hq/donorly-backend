package org.donorly.backend;

import org.donorly.backend.dto.PlatformOrgOverview;
import org.donorly.backend.dto.SetupProgressResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Payment;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.User;
import org.donorly.backend.model.UserSession;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.PaymentRepository;
import org.donorly.backend.service.PlatformMetricsService;
import org.donorly.backend.service.SetupProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Setup progress is derived from live data (no stored flags), so completing a
 * step — adding a donor, launching a campaign — must immediately move the
 * percentage. The extended /api/dashboard payload feeds the command center in
 * one round trip.
 */
class DashboardOverviewTest extends IntegrationTestBase {

    @Autowired private SetupProgressService setupProgressService;
    @Autowired private PlatformMetricsService platformMetricsService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private FollowUpRepository followUpRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void setupPercentGrowsAsStepsComplete() {
        Organization org = createOrg("Fresh Org");
        createActor(org, "organization_owner");

        SetupProgressResponse initial = setupProgressService.progressFor(org.getId());
        Map<String, SetupProgressResponse.Item> items = byKey(initial);

        assertEquals(8, initial.totalCount());
        assertTrue(items.get("profile").complete(), "name/vertical/timezone defaults make profile complete");
        assertFalse(items.get("branding").complete());
        assertFalse(items.get("team").complete(), "a single member does not count as an invited team");
        assertFalse(items.get("donors").complete());
        assertFalse(items.get("campaign").complete());
        assertFalse(items.get("pledges").complete());
        assertFalse(items.get("payments").complete());
        assertFalse(items.get("ai").complete());
        assertEquals(1, initial.completedCount());
        assertEquals(13, initial.percent(), "1 of 8 rounds to 13%");

        // Complete a few steps and verify the checklist reacts.
        Donor donor = createDonor(org, "First Donor");
        Campaign campaign = createCampaign(org, "Spring Drive");
        createPledge(org, campaign, donor, new BigDecimal("500"));
        createActor(org, "organization_admin"); // second active member
        org.setLogoUrl("https://cdn.example.org/logo.png");
        org.setPrimaryColor("#0a4f3f");
        organizationRepository.save(org);

        SetupProgressResponse after = setupProgressService.progressFor(org.getId());
        Map<String, SetupProgressResponse.Item> afterItems = byKey(after);
        assertTrue(afterItems.get("donors").complete());
        assertTrue(afterItems.get("campaign").complete());
        assertTrue(afterItems.get("pledges").complete());
        assertTrue(afterItems.get("team").complete());
        assertTrue(afterItems.get("branding").complete());
        assertEquals(6, after.completedCount());
        assertEquals(75, after.percent());
    }

    @Test
    void setupEndpointRequiresReportsView() throws Exception {
        Organization org = createOrg("Endpoint Org");
        TestActor owner = createActor(org, "organization_owner");
        TestActor volunteer = createActor(org, "volunteer");

        mockMvc.perform(get("/api/dashboard/setup").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(8)))
                .andExpect(jsonPath("$.percent").isNumber());

        mockMvc.perform(get("/api/dashboard/setup").header("Authorization", bearer(volunteer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardIncludesThermometersRecentPaymentsAndDueFollowUps() throws Exception {
        Organization org = createOrg("Command Center Org");
        TestActor owner = createActor(org, "organization_owner");
        Donor donor = createDonor(org, "Jane Giver");
        Campaign campaign = createCampaign(org, "Ramadan Appeal");
        Pledge pledge = createPledge(org, campaign, donor, new BigDecimal("1000"));

        Payment payment = new Payment();
        payment.setOrganizationId(org.getId());
        payment.setPledgeId(pledge.getId());
        payment.setDonorId(donor.getId());
        payment.setAmount(new BigDecimal("250"));
        payment.setPaymentMethod("cash");
        paymentRepository.save(payment);

        FollowUp followUp = new FollowUp();
        followUp.setOrganizationId(org.getId());
        followUp.setDonorId(donor.getId());
        followUp.setDueAt(Instant.now().plusSeconds(86400));
        followUp.setNotes("Thank-you call");
        followUpRepository.save(followUp);

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDonors").value(1))
                .andExpect(jsonPath("$.outstandingPledges").value(1))
                .andExpect(jsonPath("$.campaigns", hasSize(1)))
                .andExpect(jsonPath("$.campaigns[0].name").value("Ramadan Appeal"))
                .andExpect(jsonPath("$.campaigns[0].goalAmount").value(100000))
                .andExpect(jsonPath("$.campaigns[0].pledged").value(1000))
                .andExpect(jsonPath("$.recentPayments", hasSize(1)))
                .andExpect(jsonPath("$.recentPayments[0].donorName").value("Jane Giver"))
                .andExpect(jsonPath("$.recentPayments[0].amount").value(250))
                .andExpect(jsonPath("$.dueFollowUps", hasSize(1)))
                .andExpect(jsonPath("$.dueFollowUps[0].donorName").value("Jane Giver"))
                .andExpect(jsonPath("$.dueFollowUps[0].notes").value("Thank-you call"));
    }

    @Test
    void platformOverviewReturnsSetupPercentAndThermometerPerOrg() throws Exception {
        Organization org = createOrg("Tile Org");
        Donor donor = createDonor(org, "Tile Donor");
        Campaign campaign = createCampaign(org, "Tile Campaign"); // active, goal 100000
        createPledge(org, campaign, donor, new BigDecimal("2500"));

        PlatformOrgOverview tile = platformMetricsService.platformOverview().stream()
                .filter(o -> o.id().equals(org.getId()))
                .findFirst().orElseThrow();
        assertEquals("Tile Org", tile.name());
        assertEquals(1, tile.donorCount());
        assertEquals(1, tile.activeCampaigns());
        assertEquals(0, new BigDecimal("100000").compareTo(tile.goalTotal()));
        assertEquals(0, new BigDecimal("2500").compareTo(tile.pledgedTotal()));
        // donors + campaign + pledges + profile complete = 4 of 8
        assertEquals(50, tile.setupPercent());

        // Endpoint: platform admin claim grants access, org roles are rejected.
        User admin = new User();
        admin.setEmail("padmin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.donorly.org");
        admin.setFullName("Platform Admin");
        admin.setStatus("active");
        admin.setPlatformAdmin(true);
        admin = userRepository.save(admin);
        String jti = UUID.randomUUID().toString();
        UserSession session = new UserSession();
        session.setJti(jti);
        session.setUserId(admin.getId());
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        sessionRepository.save(session);
        String adminToken = jwtUtil.generateToken(admin.getId(), null, true, jti);

        mockMvc.perform(get("/api/organizations/overview").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setupPercent").isNumber())
                .andExpect(jsonPath("$[0].collectedTotal").isNumber());

        TestActor owner = createActor(org, "organization_owner");
        mockMvc.perform(get("/api/organizations/overview").header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void suggestionsFireFromRulesAndDismissalSnoozes() throws Exception {
        Organization org = createOrg("Suggest Org");
        TestActor owner = createActor(org, "organization_owner");

        // Fresh org: no campaign, no logo, AI off.
        mockMvc.perform(get("/api/dashboard/suggestions").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'no-active-campaign')]").exists())
                .andExpect(jsonPath("$[?(@.key == 'missing-branding')]").exists())
                .andExpect(jsonPath("$[?(@.key == 'ai-off')]").exists())
                .andExpect(jsonPath("$[?(@.key == 'overdue-followups')]").doesNotExist());

        // An overdue follow-up flips the critical rule on.
        Donor donor = createDonor(org, "Late Donor");
        FollowUp overdue = new FollowUp();
        overdue.setOrganizationId(org.getId());
        overdue.setDonorId(donor.getId());
        overdue.setDueAt(Instant.now().minusSeconds(86400));
        followUpRepository.save(overdue);

        mockMvc.perform(get("/api/dashboard/suggestions").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'overdue-followups' && @.severity == 'critical')]").exists());

        // Dismissing snoozes exactly that key; the rest keep firing.
        mockMvc.perform(post("/api/dashboard/suggestions/dismiss")
                        .header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content("{\"key\":\"ai-off\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard/suggestions").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'ai-off')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.key == 'no-active-campaign')]").exists());
    }

    @Test
    void suggestedRemindersAppearAndClearAfterSending() throws Exception {
        Organization org = createOrg("Reminder Org");
        TestActor owner = createActor(org, "organization_owner");
        Donor donor = createDonor(org, "Reminder Donor");
        donor.setEmail("reminder-donor@test.donorly.org");
        donorRepository.save(donor);
        Campaign campaign = createCampaign(org, "Reminder Campaign");
        Pledge pledge = createPledge(org, campaign, donor, new BigDecimal("800"));

        // The grace period only suggests reminders for pledges older than 7 days;
        // createdAt is stamped by the audit listener, so backdate it directly.
        jdbcTemplate.update(
                "update pledges set created_at = now() - interval '10 days' where id = ?",
                pledge.getId());

        mockMvc.perform(get("/api/pledges/suggested-reminders").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].donorName").value("Reminder Donor"))
                .andExpect(jsonPath("$[0].outstanding").value(800))
                .andExpect(jsonPath("$[0].emailBody").isNotEmpty());

        // Approving sends the email and stamps lastReminderAt — the suggestion clears.
        mockMvc.perform(post("/api/pledges/" + pledge.getId() + "/remind")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pledges/suggested-reminders").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private static Map<String, SetupProgressResponse.Item> byKey(SetupProgressResponse response) {
        return response.items().stream()
                .collect(Collectors.toMap(SetupProgressResponse.Item::key, Function.identity()));
    }
}
