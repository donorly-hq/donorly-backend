package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.SetupProgressResponse;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PaymentRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Computes an organization's onboarding progress from live data instead of
 * stored "step completed" flags. Deleting your only donor correctly reopens
 * the "Add donors" step; nothing can get stale.
 *
 * <p>Takes an explicit {@code orgId} (rather than reading {@code TenantContext})
 * so the platform overview can evaluate every organization from outside a
 * tenant-scoped request.</p>
 */
@Service
@RequiredArgsConstructor
public class SetupProgressService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public SetupProgressResponse progressFor(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        OrganizationSettings settings = settingsRepository.findById(orgId).orElse(null);

        List<SetupProgressResponse.Item> items = new ArrayList<>();

        boolean profileComplete = notBlank(org.getName()) && notBlank(org.getVertical()) && notBlank(org.getTimezone());
        items.add(new SetupProgressResponse.Item(
                "profile", "Organization profile",
                profileComplete
                        ? org.getName() + " is set up with its vertical and timezone."
                        : "Complete your organization's basic profile.",
                profileComplete, null, null));

        boolean branded = (notBlank(org.getLogoData()) || notBlank(org.getLogoUrl())) && notBlank(org.getPrimaryColor());
        items.add(new SetupProgressResponse.Item(
                "branding", "Branding",
                branded
                        ? "Your logo and brand color are live across the portal and receipts."
                        : "Ask your Donorly administrator to add your logo and brand color.",
                branded, null, null));

        boolean teamInvited = membershipRepository.countByOrganizationIdAndStatus(orgId, "active") > 1;
        items.add(new SetupProgressResponse.Item(
                "team", "Invite your team",
                teamInvited
                        ? "Your team is on board."
                        : "Add admins, campaign managers, ambassadors and volunteers.",
                teamInvited, "Invite team", "/settings/team"));

        boolean hasDonors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId) > 0;
        items.add(new SetupProgressResponse.Item(
                "donors", "Add donors",
                hasDonors
                        ? "Your donor list has started growing."
                        : "Add your first donor or import your existing list.",
                hasDonors, "Add donors", "/donors"));

        boolean hasActiveCampaign = campaignRepository.countByOrganizationIdAndStatus(orgId, "active") > 0;
        items.add(new SetupProgressResponse.Item(
                "campaign", "Launch a campaign",
                hasActiveCampaign
                        ? "You have an active fundraising campaign."
                        : "Create a campaign with a goal so pledges have a home.",
                hasActiveCampaign, "Create campaign", "/campaigns"));

        boolean hasPledges = pledgeRepository.countByOrganizationId(orgId) > 0;
        items.add(new SetupProgressResponse.Item(
                "pledges", "Record pledges",
                hasPledges
                        ? "Pledges are coming in."
                        : "Record your first pledge with the quick pledge form.",
                hasPledges, "Record a pledge", "/quick-pledge"));

        boolean hasPayments = paymentRepository.countByOrganizationId(orgId) > 0;
        items.add(new SetupProgressResponse.Item(
                "payments", "Collect payments",
                hasPayments
                        ? "You have started collecting against pledges."
                        : "Record a payment against a pledge to start tracking collections.",
                hasPayments, "Record a payment", "/payments"));

        boolean aiOn = settings != null && settings.isAiEnabled();
        items.add(new SetupProgressResponse.Item(
                "ai", "Turn on the AI assistant",
                aiOn
                        ? "AI insights and the assistant are enabled."
                        : "Enable AI to get donor insights, summaries and suggestions.",
                aiOn, "Enable AI", "/insights"));

        int completed = (int) items.stream().filter(SetupProgressResponse.Item::complete).count();
        int percent = Math.round(completed * 100f / items.size());
        return new SetupProgressResponse(percent, completed, items.size(), items);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
