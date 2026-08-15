package org.donorly.backend;

import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Photo → AI scan flow for pledge cards. Real OpenAI calls need an API key,
 * so these tests exercise everything around the model call: permission gating,
 * the org-level AI toggle, input validation, and persisting the photo plus
 * raw extraction for the audit trail.
 */
class PledgeCardScanTest extends IntegrationTestBase {

    /** 1x1 transparent PNG — a syntactically valid image data URL. */
    private static final String TINY_IMAGE =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ"
            + "AAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Autowired
    private OrganizationSettingsRepository settingsRepository;
    @Autowired
    private PledgeCardRepository pledgeCardRepository;

    private Organization org;
    private TestActor volunteer;

    @BeforeEach
    void setUp() {
        org = createOrg("Scan Org");
        volunteer = createActor(org, "volunteer");
    }

    private void enableAi() {
        OrganizationSettings settings = new OrganizationSettings();
        settings.setOrganizationId(org.getId());
        settings.setAiEnabled(true);
        settingsRepository.save(settings);
    }

    @Test
    void scanRequiresPledgesWrite() throws Exception {
        TestActor donor = createActor(org, "donor");
        mockMvc.perform(post("/api/pledge-cards/scan")
                        .header("Authorization", bearer(donor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataUrl\":\"" + TINY_IMAGE + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void scanRejectedWhenOrgAiDisabled() throws Exception {
        // No settings row → aiEnabled defaults to false.
        mockMvc.perform(post("/api/pledge-cards/scan")
                        .header("Authorization", bearer(volunteer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataUrl\":\"" + TINY_IMAGE + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("AI is not enabled")));
    }

    @Test
    void scanRejectedWhenServerHasNoApiKey() throws Exception {
        enableAi();
        // Tests run without OPENAI_API_KEY, so the gateway must fail loudly, not stub.
        mockMvc.perform(post("/api/pledge-cards/scan")
                        .header("Authorization", bearer(volunteer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataUrl\":\"" + TINY_IMAGE + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not configured")));
    }

    @Test
    void scanRejectsNonImagePayload() throws Exception {
        enableAi();
        mockMvc.perform(post("/api/pledge-cards/scan")
                        .header("Authorization", bearer(volunteer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageDataUrl\":\"data:text/plain;base64,aGVsbG8=\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("image")));
    }

    @Test
    void cardCreatedFromScanKeepsPhotoAndExtraction() throws Exception {
        String extraction = "{\\\"donor_full_name\\\":\\\"Ahmed Khan\\\",\\\"amount\\\":500}";
        String body = "{"
                + "\"donorFullName\":\"Ahmed Khan\","
                + "\"amount\":500,"
                + "\"paymentMethod\":\"cash\","
                + "\"imageUrl\":\"" + TINY_IMAGE + "\","
                + "\"extractedJson\":\"" + extraction + "\""
                + "}";

        String response = mockMvc.perform(post("/api/pledge-cards")
                        .header("Authorization", bearer(volunteer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorName").value("Ahmed Khan"))
                .andExpect(jsonPath("$.verificationStatus").value("pending"))
                .andReturn().getResponse().getContentAsString();

        UUID cardId = UUID.fromString(response.replaceAll(".*\"id\":\"([0-9a-f-]+)\".*", "$1"));
        PledgeCard saved = pledgeCardRepository.findById(cardId).orElseThrow();
        assertThat(saved.getImageUrl()).isEqualTo(TINY_IMAGE);
        assertThat(saved.getExtractedJson()).contains("Ahmed Khan");
    }
}
