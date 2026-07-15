package org.donorly.backend;

import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REQ-004 acceptance criteria: no request authenticated against Org A may read or
 * modify Org B data, regardless of role. IDs are valid UUIDs that exist in the other
 * tenant, so these tests catch any endpoint that forgets the organization filter.
 */
class CrossTenantIsolationTest extends IntegrationTestBase {

    private TestActor orgAOwner;
    private Donor orgBDonor;
    private Campaign orgBCampaign;
    private Pledge orgBPledge;

    @BeforeEach
    void setUp() {
        Organization orgA = createOrg("Org A");
        Organization orgB = createOrg("Org B");
        orgAOwner = createActor(orgA, "organization_owner");

        orgBDonor = createDonor(orgB, "Org B Donor");
        orgBCampaign = createCampaign(orgB, "Org B Campaign");
        orgBPledge = createPledge(orgB, orgBCampaign, orgBDonor, new BigDecimal("500"));
    }

    @Test
    void cannotReadAnotherOrgsDonor() throws Exception {
        mockMvc.perform(get("/api/donors/" + orgBDonor.getId())
                        .header("Authorization", bearer(orgAOwner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotReadAnotherOrgsPledge() throws Exception {
        mockMvc.perform(get("/api/pledges/" + orgBPledge.getId())
                        .header("Authorization", bearer(orgAOwner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotModifyAnotherOrgsPledge() throws Exception {
        mockMvc.perform(patch("/api/pledges/" + orgBPledge.getId())
                        .header("Authorization", bearer(orgAOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"cancelled\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteAnotherOrgsDonor() throws Exception {
        mockMvc.perform(delete("/api/donors/" + orgBDonor.getId())
                        .header("Authorization", bearer(orgAOwner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotRecordPaymentAgainstAnotherOrgsPledge() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", bearer(orgAOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pledgeId\":\"" + orgBPledge.getId()
                                + "\",\"amount\":100,\"issueReceipt\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void donorListNeverContainsOtherTenantsData() throws Exception {
        mockMvc.perform(get("/api/donors")
                        .header("Authorization", bearer(orgAOwner)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString(
                        orgBDonor.getId().toString()))));
    }

    @Test
    void cannotCreateFollowUpReferencingAnotherOrgsDonor() throws Exception {
        // Body FK validation: the donor exists but belongs to Org B, so Org A must be rejected.
        mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", bearer(orgAOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorId\":\"" + orgBDonor.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
