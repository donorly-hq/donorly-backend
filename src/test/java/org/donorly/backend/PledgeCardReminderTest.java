package org.donorly.backend;

import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorActionToken;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorActionTokenRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.donorly.backend.service.PledgeCardReminderScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The automated pledge-card chase loop: scheduler sends reminders with action
 * tokens, the public respond endpoint captures the donor's answer, and
 * exhausted attempt budgets escalate to a human follow-up.
 */
class PledgeCardReminderTest extends IntegrationTestBase {

    @Autowired private PledgeCardRepository pledgeCardRepository;
    @Autowired private OrganizationSettingsRepository settingsRepository;
    @Autowired private DonorActionTokenRepository tokenRepository;
    @Autowired private CommunicationMessageRepository messageRepository;
    @Autowired private FollowUpRepository followUpRepository;
    @Autowired private PledgeCardReminderScheduler scheduler;

    private OrganizationSettings settings(Organization org) {
        OrganizationSettings settings = new OrganizationSettings();
        settings.setOrganizationId(org.getId());
        return settingsRepository.save(settings);
    }

    private PledgeCard card(Organization org, Campaign campaign, Donor donor, Instant pendingSince) {
        PledgeCard card = new PledgeCard();
        card.setOrganizationId(org.getId());
        card.setCampaignId(campaign != null ? campaign.getId() : null);
        card.setDonorId(donor.getId());
        card.setAmount(new BigDecimal("500"));
        card.setVerificationStatus("needs_verification"); // Stays out of the auto-approve path.
        card.setPendingSince(pendingSince);
        return pledgeCardRepository.save(card);
    }

    @Test
    void schedulerSendsReminderWithActionToken() throws Exception {
        Organization org = createOrg("Reminder Org");
        settings(org);
        Campaign campaign = createCampaign(org, "Ramadan Drive");
        Donor donor = createDonor(org, "Reminded Donor");
        donor.setEmail("reminded@donors.test");
        donorRepository.save(donor);
        PledgeCard card = card(org, campaign, donor, Instant.now().minus(Duration.ofDays(4)));

        scheduler.sendPledgeCardReminders();

        PledgeCard after = pledgeCardRepository.findById(card.getId()).orElseThrow();
        assertNotNull(after.getLastReminderAt());
        assertEquals(1, after.getFollowUpCount());

        List<DonorActionToken> tokens = tokenRepository.findByPledgeCardId(card.getId());
        assertEquals(1, tokens.size());

        boolean outboundRecorded = messageRepository.findAll().stream()
                .anyMatch(m -> donor.getId().equals(m.getDonorId())
                        && "outbound".equals(m.getDirection())
                        && m.getBody() != null && m.getBody().contains(tokens.get(0).getToken()));
        assertTrue(outboundRecorded, "outbound reminder should be in the communication history");

        // Within the interval nothing new goes out.
        scheduler.sendPledgeCardReminders();
        assertEquals(1, tokenRepository.findByPledgeCardId(card.getId()).size());
    }

    @Test
    void donorRespondsPaidAndCardEntersVerificationQueue() throws Exception {
        Organization org = createOrg("Respond Org");
        settings(org);
        Campaign campaign = createCampaign(org, "Masjid Fund");
        Donor donor = createDonor(org, "Paying Donor");
        donor.setEmail("paying@donors.test");
        donorRepository.save(donor);
        PledgeCard card = card(org, campaign, donor, Instant.now().minus(Duration.ofDays(4)));

        scheduler.sendPledgeCardReminders();
        String token = tokenRepository.findByPledgeCardId(card.getId()).get(0).getToken();

        mockMvc.perform(get("/api/public/respond/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgName").value("Respond Org"))
                .andExpect(jsonPath("$.donorFirstName").value("Paying"))
                .andExpect(jsonPath("$.used").value(false));

        mockMvc.perform(post("/api/public/respond/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"paid\",\"comment\":\"Paid by check last week\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        PledgeCard after = pledgeCardRepository.findById(card.getId()).orElseThrow();
        assertEquals("claims_paid", after.getVerificationStatus());
        assertTrue(after.isRemindersPaused());

        boolean inboundRecorded = messageRepository.findAll().stream()
                .anyMatch(m -> donor.getId().equals(m.getDonorId())
                        && "inbound".equals(m.getDirection())
                        && m.getBody().contains("already paid"));
        assertTrue(inboundRecorded, "the response should appear as an inbound message");

        boolean verifyTask = followUpRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(f -> "open".equals(f.getStatus()) && f.getNotes().contains("verify"));
        assertTrue(verifyTask, "a verification follow-up should be created");

        // Token is single-use.
        mockMvc.perform(post("/api/public/respond/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"paid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void donorPromiseStoresDateAndPausesReminders() throws Exception {
        Organization org = createOrg("Promise Org");
        settings(org);
        Donor donor = createDonor(org, "Promising Donor");
        donor.setEmail("promising@donors.test");
        donorRepository.save(donor);
        PledgeCard card = card(org, null, donor, Instant.now().minus(Duration.ofDays(4)));

        scheduler.sendPledgeCardReminders();
        String token = tokenRepository.findByPledgeCardId(card.getId()).get(0).getToken();
        LocalDate promised = LocalDate.now().plusDays(14);

        mockMvc.perform(post("/api/public/respond/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"promise\",\"promiseDate\":\"" + promised + "\"}"))
                .andExpect(status().isOk());

        PledgeCard after = pledgeCardRepository.findById(card.getId()).orElseThrow();
        assertEquals(promised, after.getPromisedDate());
        assertTrue(after.isRemindersPaused());

        boolean checkIn = followUpRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(f -> f.getNotes().contains(promised.toString()));
        assertTrue(checkIn, "a follow-up on the promised date should exist");
    }

    @Test
    void exhaustedAttemptsEscalateToHumanFollowUp() throws Exception {
        Organization org = createOrg("Escalate Org");
        settings(org); // default max = 3
        Donor donor = createDonor(org, "Silent Donor");
        donor.setEmail("silent@donors.test");
        donorRepository.save(donor);
        PledgeCard card = card(org, null, donor, Instant.now().minus(Duration.ofDays(30)));
        card.setFollowUpCount(3);
        pledgeCardRepository.save(card);

        scheduler.sendPledgeCardReminders();

        PledgeCard after = pledgeCardRepository.findById(card.getId()).orElseThrow();
        assertEquals("non_responsive", after.getVerificationStatus());
        boolean escalation = followUpRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(f -> f.getNotes().contains("unresponsive after 3 automated reminders"));
        assertTrue(escalation, "an escalation follow-up should be created");

        // Escalation happens once, not every day.
        scheduler.sendPledgeCardReminders();
        long count = followUpRepository.findByOrganizationId(org.getId()).stream()
                .filter(f -> f.getNotes().contains("unresponsive")).count();
        assertEquals(1, count);
    }

    @Test
    void reminderPolicyRoundTripsAndDisablingStopsSends() throws Exception {
        Organization org = createOrg("Policy Org");
        settings(org);
        TestActor owner = createActor(org, "organization_owner");

        mockMvc.perform(put("/api/pledge-cards/reminder-policy")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"intervalDays\":5,\"maxAttempts\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.intervalDays").value(5))
                .andExpect(jsonPath("$.maxAttempts").value(2));

        Donor donor = createDonor(org, "Unbothered Donor");
        donor.setEmail("unbothered@donors.test");
        donorRepository.save(donor);
        PledgeCard card = card(org, null, donor, Instant.now().minus(Duration.ofDays(10)));

        scheduler.sendPledgeCardReminders();

        PledgeCard after = pledgeCardRepository.findById(card.getId()).orElseThrow();
        assertFalse(after.getLastReminderAt() != null, "no reminder when the org disabled them");

        mockMvc.perform(get("/api/pledge-cards/reminder-policy")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
