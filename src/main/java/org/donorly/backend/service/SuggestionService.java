package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.SuggestionResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.DismissedSuggestion;
import org.donorly.backend.model.Organization;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DismissedSuggestionRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rule-based "AI suggestions" engine (Observe → Discover → Recommend).
 * Suggestions are recomputed from live data on every request — nothing stored
 * can go stale. Only dismissals persist: dismissing snoozes one suggestion key
 * for {@link #DISMISS_TTL}, after which the rule re-fires if still true.
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    static final Duration DISMISS_TTL = Duration.ofDays(30);
    private static final Duration DORMANT_DONOR_WINDOW = Duration.ofDays(180);
    private static final Duration STALE_PLEDGE_AGE = Duration.ofDays(30);
    private static final int CAMPAIGN_ENDING_SOON_DAYS = 30;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final FollowUpRepository followUpRepository;
    private final DonorRepository donorRepository;
    private final DismissedSuggestionRepository dismissedRepository;

    @Transactional(readOnly = true)
    public List<SuggestionResponse> currentSuggestions() {
        UUID orgId = TenantContext.requireOrganizationId();
        Instant now = Instant.now();

        Set<String> snoozed = dismissedRepository.findByOrganizationId(orgId).stream()
                .filter(d -> d.getDismissedUntil().isAfter(now))
                .map(DismissedSuggestion::getSuggestionKey)
                .collect(Collectors.toSet());

        List<SuggestionResponse> suggestions = new ArrayList<>();

        long overdue = followUpRepository.countOverdue(orgId, now);
        if (overdue > 0) {
            suggestions.add(new SuggestionResponse(
                    "overdue-followups", "critical", "Follow-ups are overdue",
                    overdue + " follow-up" + plural(overdue) + " past the due date. Donors respond best to timely contact.",
                    "Review follow-ups", "/follow-ups"));
        }

        long stale = pledgeRepository.countStaleByOrganization(
                orgId, now.minus(STALE_PLEDGE_AGE), now.minus(STALE_PLEDGE_AGE));
        if (stale > 0) {
            suggestions.add(new SuggestionResponse(
                    "stale-pledges", "warning", "Pledges have gone quiet",
                    stale + " unpaid pledge" + plural(stale) + " older than 30 days with no recent reminder.",
                    "Review pledges", "/pledge-cards"));
        }

        List<Campaign> activeCampaigns = campaignRepository
                .findByOrganizationIdAndStatusOrderByStartDateAsc(orgId, "active");
        if (activeCampaigns.isEmpty()) {
            suggestions.add(new SuggestionResponse(
                    "no-active-campaign", "warning", "No active campaign",
                    "There is nowhere for new pledges to go. Launch a campaign with a clear goal.",
                    "Create a campaign", "/campaigns"));
        } else {
            LocalDate soon = LocalDate.now().plusDays(CAMPAIGN_ENDING_SOON_DAYS);
            for (Campaign c : activeCampaigns) {
                if (c.getEndDate() == null || c.getEndDate().isAfter(soon)) continue;
                if (c.getGoalAmount().signum() <= 0) continue;
                BigDecimal collected = nz(pledgeRepository.sumCollectedByCampaign(orgId, c.getId()));
                // Behind = under half the goal with under a month to go.
                if (collected.multiply(BigDecimal.valueOf(2)).compareTo(c.getGoalAmount()) < 0) {
                    int pct = collected.multiply(BigDecimal.valueOf(100))
                            .divide(c.getGoalAmount(), java.math.RoundingMode.HALF_UP).intValue();
                    suggestions.add(new SuggestionResponse(
                            "campaign-behind:" + c.getId(), "warning",
                            "\"" + c.getName() + "\" is behind its goal",
                            "It ends " + c.getEndDate() + " with only " + pct + "% of the goal collected. "
                                    + "Consider a reminder push or extending the end date.",
                            "Open campaign", "/campaigns/" + c.getId()));
                }
            }
        }

        long dormant = donorRepository.countDormantSince(orgId, now.minus(DORMANT_DONOR_WINDOW));
        if (dormant > 0) {
            String verb = dormant == 1 ? " donor who gave before hasn't" : " donors who gave before haven't";
            suggestions.add(new SuggestionResponse(
                    "dormant-donors", "info", "Past donors have gone dormant",
                    dormant + verb + " donated in over 6 months. A personal follow-up often re-engages them.",
                    "View donors", "/donors"));
        }

        Organization org = organizationRepository.findById(orgId).orElse(null);
        boolean hasLogo = org != null
                && ((org.getLogoData() != null && !org.getLogoData().isBlank())
                    || (org.getLogoUrl() != null && !org.getLogoUrl().isBlank()));
        if (org != null && !hasLogo) {
            suggestions.add(new SuggestionResponse(
                    "missing-branding", "info", "Your logo is missing",
                    "Receipts and pledge pages look more trustworthy with your logo. "
                            + "Ask your Donorly administrator to add it.",
                    null, null));
        }

        boolean aiOn = settingsRepository.findById(orgId)
                .map(s -> s.isAiEnabled()).orElse(false);
        if (!aiOn) {
            suggestions.add(new SuggestionResponse(
                    "ai-off", "info", "AI assistant is turned off",
                    "Enable AI to get donor summaries, campaign insights and a fundraising assistant.",
                    "Enable AI", "/insights"));
        }

        return suggestions.stream()
                .filter(s -> !snoozed.contains(s.key()))
                .toList();
    }

    @Transactional
    public void dismiss(String key) {
        UUID orgId = TenantContext.requireOrganizationId();
        DismissedSuggestion dismissal = dismissedRepository
                .findByOrganizationIdAndSuggestionKey(orgId, key)
                .orElseGet(() -> {
                    DismissedSuggestion d = new DismissedSuggestion();
                    d.setOrganizationId(orgId);
                    d.setSuggestionKey(key);
                    return d;
                });
        dismissal.setDismissedBy(TenantContext.getUserId());
        dismissal.setDismissedUntil(Instant.now().plus(DISMISS_TTL));
        dismissedRepository.save(dismissal);
    }

    private static String plural(long n) {
        return n == 1 ? " is" : "s are";
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
