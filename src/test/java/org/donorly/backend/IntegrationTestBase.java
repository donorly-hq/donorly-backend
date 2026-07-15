package org.donorly.backend;

import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationMembership;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.Role;
import org.donorly.backend.model.User;
import org.donorly.backend.model.UserSession;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.repository.RoleRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.repository.UserSessionRepository;
import org.donorly.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Base for integration tests. Boots the full application (real security filter chain,
 * real Flyway schema) against a disposable Postgres started by Testcontainers.
 *
 * The container uses the singleton pattern (started once in a static initializer and
 * shared by every test class) so the Spring context can be cached across classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JwtUtil jwtUtil;
    @Autowired protected OrganizationRepository organizationRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected OrganizationMembershipRepository membershipRepository;
    @Autowired protected UserSessionRepository sessionRepository;
    @Autowired protected DonorRepository donorRepository;
    @Autowired protected CampaignRepository campaignRepository;
    @Autowired protected PledgeRepository pledgeRepository;

    /** A user with an org membership and a live session; {@code token} is a valid JWT. */
    public record TestActor(User user, Organization org, String token) {
    }

    protected Organization createOrg(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + shortId());
        org.setStatus("active");
        return organizationRepository.save(org);
    }

    /** Creates a user, an active membership with the given role, and a session token. */
    protected TestActor createActor(Organization org, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleCode));

        User user = new User();
        user.setEmail(roleCode + "-" + shortId() + "@test.donorly.org");
        user.setFullName("Test " + roleCode);
        user.setStatus("active");
        user = userRepository.save(user);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganizationId(org.getId());
        membership.setUserId(user.getId());
        membership.setRoleId(role.getId());
        membership.setStatus("active");
        membershipRepository.save(membership);

        String jti = UUID.randomUUID().toString();
        UserSession session = new UserSession();
        session.setJti(jti);
        session.setUserId(user.getId());
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        sessionRepository.save(session);

        String token = jwtUtil.generateToken(user.getId(), org.getId(), false, jti);
        return new TestActor(user, org, token);
    }

    protected Donor createDonor(Organization org, String name) {
        Donor donor = new Donor();
        donor.setOrganizationId(org.getId());
        donor.setFullName(name);
        return donorRepository.save(donor);
    }

    protected Campaign createCampaign(Organization org, String name) {
        Campaign campaign = new Campaign();
        campaign.setOrganizationId(org.getId());
        campaign.setName(name);
        campaign.setSlug(name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + shortId());
        campaign.setStatus("active");
        campaign.setGoalAmount(new BigDecimal("100000"));
        return campaignRepository.save(campaign);
    }

    protected Pledge createPledge(Organization org, Campaign campaign, Donor donor, BigDecimal amount) {
        Pledge pledge = new Pledge();
        pledge.setOrganizationId(org.getId());
        pledge.setCampaignId(campaign.getId());
        pledge.setDonorId(donor.getId());
        pledge.setAmount(amount);
        pledge.setStatus("active");
        return pledgeRepository.save(pledge);
    }

    protected static String bearer(TestActor actor) {
        return "Bearer " + actor.token();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
