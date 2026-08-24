package org.donorly.backend;

import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.PledgeCardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bulk pledge-card import: existing donors are matched (not duplicated),
 * campaign names are fuzzy-matched with a default fallback, bad rows are
 * reported, and duplicate rows within the file are skipped.
 */
class PledgeCardImportTest extends IntegrationTestBase {

    @Autowired private PledgeCardRepository pledgeCardRepository;

    @Test
    void importMatchesDonorsAndCampaignsAndReportsBadRows() throws Exception {
        Organization org = createOrg("Import Org");
        TestActor owner = createActor(org, "organization_owner");
        Campaign ramadan = createCampaign(org, "Ramadan Drive 2026");
        Campaign general = createCampaign(org, "General Fund");

        Donor existing = createDonor(org, "Existing Donor");
        existing.setEmail("existing@donors.test");
        donorRepository.save(existing);
        long donorsBefore = donorRepository.findByOrganizationIdAndDeletedAtIsNull(org.getId()).size();

        String body = """
                {"cards":[
                  {"donorFullName":"Existing Donor","donorEmail":"EXISTING@donors.test","amount":500,
                   "paymentMethod":"cheque"},
                  {"donorFullName":"Brand New Donor","donorPhone":"555-0101","amount":1200,
                   "campaignName":"ramadan","donorType":"organization"},
                  {"donorFullName":"No Amount Donor"},
                  {"donorFullName":"Brand New Donor","donorPhone":"555-0101","amount":1200,
                   "campaignName":"Ramadan Drive 2026"}
                ],
                "defaultCampaignId":"%s","batch":"Sheet A"}
                """.formatted(general.getId());

        mockMvc.perform(post("/api/pledge-cards/import")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0]").value("Row 3: a positive amount is required"));

        List<PledgeCard> cards = pledgeCardRepository.findByOrganizationId(org.getId());
        assertEquals(2, cards.size());
        assertTrue(cards.stream().allMatch(c -> "pending".equals(c.getVerificationStatus())));
        assertTrue(cards.stream().allMatch(c -> "Sheet A".equals(c.getBatch())));

        // Row 1 reused the existing donor (matched by email, case-insensitive).
        PledgeCard existingDonorCard = cards.stream()
                .filter(c -> existing.getId().equals(c.getDonorId()))
                .findFirst().orElseThrow();
        // No campaign name on the row, so the default campaign applies.
        assertEquals(general.getId(), existingDonorCard.getCampaignId());
        assertEquals("check", existingDonorCard.getPaymentMethod());

        // Row 2 created exactly one new donor and fuzzy-matched "ramadan".
        var donorsAfter = donorRepository.findByOrganizationIdAndDeletedAtIsNull(org.getId());
        assertEquals(donorsBefore + 1, donorsAfter.size());
        Donor created = donorsAfter.stream()
                .filter(d -> "Brand New Donor".equals(d.getFullName()))
                .findFirst().orElseThrow();
        assertEquals("business", created.getDonorType());
        PledgeCard newDonorCard = cards.stream()
                .filter(c -> created.getId().equals(c.getDonorId()))
                .findFirst().orElseThrow();
        assertEquals(ramadan.getId(), newDonorCard.getCampaignId());
    }

    @Test
    void importWithoutDefaultCampaignLeavesCardUnlinked() throws Exception {
        Organization org = createOrg("No Default Org");
        TestActor owner = createActor(org, "organization_owner");

        mockMvc.perform(post("/api/pledge-cards/import")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cards\":[{\"donorFullName\":\"Loose Donor\",\"amount\":100}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));

        List<PledgeCard> cards = pledgeCardRepository.findByOrganizationId(org.getId());
        assertEquals(1, cards.size());
        assertNull(cards.get(0).getCampaignId());
        assertEquals("pending", cards.get(0).getVerificationStatus());
    }
}
