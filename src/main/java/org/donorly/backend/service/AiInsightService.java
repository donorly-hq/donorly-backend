package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final AiGateway aiGateway;
    private final AiInsightRepository insightRepository;
    private final AiConversationRepository conversationRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final FollowUpRepository followUpRepository;
    private final DonorNoteRepository donorNoteRepository;
    private final AuditService auditService;

    // ---- Org AI settings ---------------------------------------------------

    public boolean isAiEnabled() {
        UUID orgId = TenantContext.requireOrganizationId();
        return settingsRepository.findById(orgId)
                .map(OrganizationSettings::isAiEnabled)
                .orElse(false);
    }

    @Transactional
    public void setAiEnabled(boolean enabled) {
        UUID orgId = TenantContext.requireOrganizationId();
        OrganizationSettings settings = settingsRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization settings not found"));
        settings.setAiEnabled(enabled);
        settingsRepository.save(settings);
        auditService.record("ai.settings.update", "organization_settings", orgId);
    }

    // ---- Donor insight generation ------------------------------------------

    @Transactional
    public AiInsight generateDonorInsight(UUID donorId) {
        requireAiEnabled();
        UUID orgId = TenantContext.requireOrganizationId();
        Donor donor = donorRepository.findByIdAndOrganizationId(donorId, orgId)
                .orElseThrow(() -> new NotFoundException("Donor not found"));
        Organization org = getOrg(orgId);

        List<Pledge> pledges = pledgeRepository.findByOrganizationIdAndDonorId(orgId, donorId);
        List<FollowUp> followUps = followUpRepository.findByOrganizationId(orgId).stream()
                .filter(f -> donorId.equals(f.getDonorId())).toList();
        List<DonorNote> notes = donorNoteRepository.findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(orgId, donorId);

        BigDecimal totalPledged = pledges.stream()
                .map(Pledge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = pledges.stream()
                .map(Pledge::getCollectedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long openFollowUps = followUps.stream().filter(f -> "open".equals(f.getStatus())).count();

        String systemPrompt = """
                You are a helpful fundraising assistant for %s. Analyze donor data and provide concise,
                actionable insights in 3-5 bullet points. Focus on giving patterns, engagement level,
                recommended next steps, and suggested ask amount. Be specific and practical. Keep it under 200 words.
                """.formatted(org.getName());

        String userPrompt = """
                Donor: %s
                City: %s
                Donor type: %s
                Total pledged: $%s
                Total collected: $%s
                Number of pledges: %d
                Open follow-ups: %d
                Total notes on file: %d
                Recent pledge statuses: %s
                """.formatted(
                donor.getFullName(),
                donor.getCity() != null ? donor.getCity() : "Unknown",
                donor.getDonorType(),
                totalPledged.toPlainString(),
                totalCollected.toPlainString(),
                pledges.size(),
                openFollowUps,
                notes.size(),
                pledges.stream().map(Pledge::getStatus).distinct().toList()
        );

        String insight = aiGateway.chat(systemPrompt, userPrompt);

        AiInsight saved = persist(orgId, "donor", donorId, insight);
        auditService.record("ai.insight.donor", "donor", donorId);
        return saved;
    }

    public Optional<AiInsight> latestDonorInsight(UUID donorId) {
        UUID orgId = TenantContext.requireOrganizationId();
        return insightRepository.findTop1ByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                orgId, "donor", donorId).stream().findFirst();
    }

    // ---- Campaign insight generation ---------------------------------------

    @Transactional
    public AiInsight generateCampaignInsight(UUID campaignId) {
        requireAiEnabled();
        UUID orgId = TenantContext.requireOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        Organization org = getOrg(orgId);

        List<Pledge> pledges = pledgeRepository.findByOrganizationIdAndCampaignId(orgId, campaignId);
        BigDecimal pledged = pledgeRepository.sumPledgedByCampaign(orgId, campaignId);
        BigDecimal collected = pledgeRepository.sumCollectedByCampaign(orgId, campaignId);
        long pct = campaign.getGoalAmount().compareTo(BigDecimal.ZERO) > 0
                ? collected.multiply(BigDecimal.valueOf(100)).divide(campaign.getGoalAmount(), 0, java.math.RoundingMode.HALF_UP).longValue()
                : 0;

        String systemPrompt = """
                You are a helpful fundraising analyst for %s. Given campaign metrics, provide 3-5 concise,
                actionable insights: performance vs goal, trends, risk factors, and specific recommended actions.
                Keep it under 200 words.
                """.formatted(org.getName());

        String userPrompt = """
                Campaign: %s
                Type: %s
                Status: %s
                Goal: $%s
                Total pledged: $%s
                Total collected: $%s
                Collected vs goal: %d%%
                Number of pledges: %d
                Start date: %s
                End date: %s
                """.formatted(
                campaign.getName(),
                campaign.getCampaignType(),
                campaign.getStatus(),
                campaign.getGoalAmount().toPlainString(),
                pledged.toPlainString(),
                collected.toPlainString(),
                pct,
                pledges.size(),
                campaign.getStartDate() != null ? campaign.getStartDate().toString() : "Not set",
                campaign.getEndDate() != null ? campaign.getEndDate().toString() : "Not set"
        );

        String insight = aiGateway.chat(systemPrompt, userPrompt);
        AiInsight saved = persist(orgId, "campaign", campaignId, insight);
        auditService.record("ai.insight.campaign", "campaign", campaignId);
        return saved;
    }

    public Optional<AiInsight> latestCampaignInsight(UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        return insightRepository.findTop1ByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                orgId, "campaign", campaignId).stream().findFirst();
    }

    // ---- Org-level insight -------------------------------------------------

    @Transactional
    public AiInsight generateOrgInsight() {
        requireAiEnabled();
        UUID orgId = TenantContext.requireOrganizationId();
        Organization org = getOrg(orgId);

        long donors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long campaigns = campaignRepository.findByOrganizationId(orgId).size();
        BigDecimal pledged = pledgeRepository.sumPledgedByOrganization(orgId);
        BigDecimal collected = pledgeRepository.sumCollectedByOrganization(orgId);
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");

        String systemPrompt = """
                You are a senior fundraising advisor for %s. Given org-wide metrics, provide an executive
                summary with 4-6 bullet points: overall fundraising health, collection efficiency,
                engagement indicators, and top 2 recommended priorities. Under 250 words.
                """.formatted(org.getName());

        String userPrompt = """
                Organization: %s
                Total donors: %d
                Active campaigns: %d
                Total pledged: $%s
                Total collected: $%s
                Collection rate: %s%%
                Open follow-ups: %d
                """.formatted(
                org.getName(), donors, campaigns,
                pledged.toPlainString(), collected.toPlainString(),
                pledged.compareTo(BigDecimal.ZERO) > 0
                        ? collected.multiply(BigDecimal.valueOf(100)).divide(pledged, 0, java.math.RoundingMode.HALF_UP).toPlainString()
                        : "0",
                openFollowUps
        );

        String insight = aiGateway.chat(systemPrompt, userPrompt);
        AiInsight saved = persist(orgId, "org", null, insight);
        auditService.record("ai.insight.org", "organization", orgId);
        return saved;
    }

    public Optional<AiInsight> latestOrgInsight() {
        UUID orgId = TenantContext.requireOrganizationId();
        return insightRepository.findTop1ByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(orgId, "org");
    }

    // ---- Free-form chat assistant ------------------------------------------

    @Transactional
    public AiConversation ask(String question) {
        requireAiEnabled();
        UUID orgId = TenantContext.requireOrganizationId();
        Organization org = getOrg(orgId);

        long donors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");
        BigDecimal pledged = pledgeRepository.sumPledgedByOrganization(orgId);
        BigDecimal collected = pledgeRepository.sumCollectedByOrganization(orgId);

        String systemPrompt = """
                You are Donorly AI, an intelligent assistant for the fundraising team at %s.
                You help staff understand donor data, campaign performance, and best practices.
                You have access to the following live org summary:
                  - Total donors: %d
                  - Open follow-ups: %d
                  - Total pledged: $%s
                  - Total collected: $%s
                Answer concisely and practically. If you don't have enough data to answer confidently, say so.
                """.formatted(org.getName(), donors, openFollowUps,
                pledged.toPlainString(), collected.toPlainString());

        AiConversation conversation = new AiConversation();
        conversation.setOrganizationId(orgId);
        conversation.setUserId(TenantContext.getUserId());
        conversation.setQuestion(question);
        conversation.setStatus("pending");
        conversation = conversationRepository.save(conversation);

        try {
            String answer = aiGateway.chat(systemPrompt, question);
            conversation.setAnswer(answer);
            conversation.setStatus("answered");
            conversation.setModel(aiGateway.modelName());
        } catch (Exception e) {
            conversation.setAnswer("Sorry, I couldn't process that question right now.");
            conversation.setStatus("failed");
        }

        auditService.record("ai.ask", "ai_conversation", conversation.getId());
        return conversationRepository.save(conversation);
    }

    public List<AiConversation> recentConversations() {
        return conversationRepository.findTop20ByOrganizationIdOrderByCreatedAtDesc(
                TenantContext.requireOrganizationId());
    }

    public List<AiInsight> recentInsights() {
        return insightRepository.findByOrganizationIdOrderByCreatedAtDesc(
                TenantContext.requireOrganizationId());
    }

    // ---- helpers -----------------------------------------------------------

    private AiInsight persist(UUID orgId, String entityType, UUID entityId, String insight) {
        AiInsight ai = new AiInsight();
        ai.setOrganizationId(orgId);
        ai.setEntityType(entityType);
        ai.setEntityId(entityId);
        ai.setInsight(insight);
        ai.setModel(aiGateway.modelName());
        ai.setGeneratedBy(TenantContext.getUserId());
        return insightRepository.save(ai);
    }

    private void requireAiEnabled() {
        if (!isAiEnabled()) {
            throw new BadRequestException("AI is not enabled for this organization. Turn it on in Settings.");
        }
    }

    private Organization getOrg(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }
}
