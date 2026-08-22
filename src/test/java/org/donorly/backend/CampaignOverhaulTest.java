package org.donorly.backend;

import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for the campaign/donor/payments/pledge-card overhaul:
 * audience targeting, messaging config, donor filters, direct campaign
 * payments (no pledge), pledge-card approve-to-pledge conversion, the
 * auto-approve policy, and the public Twilio inbound webhook.
 */
class CampaignOverhaulTest extends IntegrationTestBase {

    @Autowired private OrganizationSettingsRepository settingsRepository;
    @Autowired private PledgeCardRepository pledgeCardRepository;

    @Test
    void audienceTargetingResolvesDistinctDonorCount() throws Exception {
        Organization org = createOrg("Audience Org");
        TestActor owner = createActor(org, "organization_owner");
        Campaign campaign = createCampaign(org, "Ramadan Drive");
        Donor donor = createDonor(org, "Targeted Donor");

        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/audience")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorId\":\"" + donor.getId() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/campaigns/" + campaign.getId() + "/audience")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targets.length()").value(1))
                .andExpect(jsonPath("$.targetedDonorCount").value(1));
    }

    @Test
    void messagingConfigRoundTrips() throws Exception {
        Organization org = createOrg("Messaging Org");
        TestActor owner = createActor(org, "organization_owner");
        Campaign campaign = createCampaign(org, "Masjid Expansion");

        mockMvc.perform(put("/api/campaigns/" + campaign.getId() + "/messaging")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageContent":"Salaam {donor_name}, day {campaign_day}!",
                                 "frequency":"every_2_days",
                                 "channels":["email","whatsapp"],
                                 "personalized":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("every_2_days"));

        mockMvc.perform(get("/api/campaigns/" + campaign.getId() + "/messaging")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels.length()").value(2))
                .andExpect(jsonPath("$.personalized").value(true));
    }

    @Test
    void donorBucketFilterNarrowsTheList() throws Exception {
        Organization org = createOrg("Filter Org");
        TestActor owner = createActor(org, "organization_owner");
        createDonor(org, "Confirmed Donor"); // default bucket = confirmed
        Donor potential = createDonor(org, "Potential Donor");
        potential.setBucket("potential");
        donorRepository.save(potential);

        mockMvc.perform(get("/api/donors?page=0&size=50&bucket=potential")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Potential Donor"));
    }

    @Test
    void directCampaignPaymentNeedsNoPledge() throws Exception {
        Organization org = createOrg("Takaza Org");
        TestActor owner = createActor(org, "organization_owner");
        Campaign campaign = createCampaign(org, "Friday Collection");
        Donor donor = createDonor(org, "Walk-in Donor");

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignId\":\"" + campaign.getId() + "\","
                                + "\"donorId\":\"" + donor.getId() + "\","
                                + "\"amount\":250,\"paymentMethod\":\"cash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pledgeId").isEmpty())
                .andExpect(jsonPath("$.campaignId").value(campaign.getId().toString()));
    }

    @Test
    void approvingPledgeCardConvertsItToPledge() throws Exception {
        Organization org = createOrg("Pilot Org");
        TestActor owner = createActor(org, "organization_owner");
        Campaign campaign = createCampaign(org, "Pilot Campaign");
        Donor donor = createDonor(org, "Card Donor");

        String cardJson = mockMvc.perform(post("/api/pledge-cards")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignId\":\"" + campaign.getId() + "\","
                                + "\"donorId\":\"" + donor.getId() + "\","
                                + "\"donorFullName\":\"Card Donor\","
                                + "\"amount\":1200,\"batch\":\"Pilot 1200\"}"))
                .andExpect(status().isOk())
                // POC defaults to whoever entered the card.
                .andExpect(jsonPath("$.pointOfContactUserId").value(owner.user().getId().toString()))
                .andExpect(jsonPath("$.batch").value("Pilot 1200"))
                .andReturn().getResponse().getContentAsString();
        String cardId = cardJson.replaceAll(".*\"id\":\"([0-9a-f-]+)\".*", "$1");

        mockMvc.perform(patch("/api/pledge-cards/" + cardId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("approved"));

        List<Pledge> pledges = pledgeRepository.findByOrganizationId(org.getId());
        assertEquals(1, pledges.size(), "approval must create exactly one pledge");
        assertEquals(0, new BigDecimal("1200").compareTo(pledges.get(0).getAmount()));
        assertEquals("pledge_card", pledges.get(0).getSource());
        assertEquals(donor.getId(), pledges.get(0).getDonorId());
    }

    @Test
    void autoApprovePolicyRoundTrips() throws Exception {
        Organization org = createOrg("Policy Org");
        TestActor owner = createActor(org, "organization_owner");
        OrganizationSettings settings = new OrganizationSettings();
        settings.setOrganizationId(org.getId());
        settingsRepository.save(settings);

        mockMvc.perform(put("/api/pledge-cards/auto-approve-policy")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoApprove\":true,\"hours\":48}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hours").value(48));

        mockMvc.perform(get("/api/pledge-cards/auto-approve-policy")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoApprove").value(true))
                .andExpect(jsonPath("$.hours").value(48));
    }

    @Test
    void twilioInboundWebhookAnswersWithTwiml() throws Exception {
        String xml = mockMvc.perform(post("/api/public/twilio/inbound")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+15550001111")
                        .param("Body", "1")
                        .param("MessageSid", "SM_test_123"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(xml.contains("<Response><Message>"), "reply must be TwiML");
    }
}
