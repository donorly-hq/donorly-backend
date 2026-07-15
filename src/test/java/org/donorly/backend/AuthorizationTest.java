package org.donorly.backend;

import org.donorly.backend.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-based authorization on the endpoints hardened in the July 2026 review:
 * the org-wide financial dashboard and the pledge CSV export must not be readable
 * by low-privilege roles (volunteer), while org admins keep access.
 */
class AuthorizationTest extends IntegrationTestBase {

    private TestActor volunteer;
    private TestActor orgAdmin;

    @BeforeEach
    void setUp() {
        Organization org = createOrg("Authz Org");
        volunteer = createActor(org, "volunteer");
        orgAdmin = createActor(org, "organization_admin");
    }

    @Test
    void volunteerCannotSeeOrgWideDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(volunteer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void orgAdminCanSeeOrgWideDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(orgAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void volunteerCannotExportPledges() throws Exception {
        mockMvc.perform(get("/api/export/pledges").header("Authorization", bearer(volunteer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void orgAdminCanExportPledges() throws Exception {
        mockMvc.perform(get("/api/export/pledges").header("Authorization", bearer(orgAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void volunteerCannotExportDonors() throws Exception {
        mockMvc.perform(get("/api/export/donors").header("Authorization", bearer(volunteer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/donors"))
                .andExpect(status().isUnauthorized());
    }
}
