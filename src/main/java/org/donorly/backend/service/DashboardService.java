package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.AmbassadorDashboardResponse;
import org.donorly.backend.dto.CampaignDashboardResponse;
import org.donorly.backend.dto.CampaignManagerDashboardResponse;
import org.donorly.backend.dto.OrgDashboardResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.DonorAssignment;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorAssignmentRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.EventRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.repository.RoleRepository;
import org.donorly.backend.repository.TownhallRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.model.Role;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DonorRepository donorRepository;
    private final DonorAssignmentRepository donorAssignmentRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final FollowUpRepository followUpRepository;
    private final org.donorly.backend.repository.PaymentRepository paymentRepository;
    private final EventRepository eventRepository;
    private final TownhallRepository townhallRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CampaignProgressService campaignProgressService;
    private final org.donorly.backend.repository.CampaignTargetRepository campaignTargetRepository;

    public OrgDashboardResponse orgDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        long totalDonors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long totalCampaigns = campaignRepository.findByOrganizationId(orgId).size();
        BigDecimal pledged = nz(pledgeRepository.sumPledgedByOrganization(orgId));
        BigDecimal collected = nz(pledgeRepository.sumCollectedByOrganization(orgId));
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");
        long outstandingPledges = pledgeRepository.countOutstandingByOrganization(orgId);

        // Thermometers: active campaigns, goal vs pledged/collected.
        List<OrgDashboardResponse.CampaignProgress> campaigns = campaignRepository
                .findByOrganizationIdAndStatusOrderByStartDateAsc(orgId, "active")
                .stream()
                .limit(6)
                .map(c -> {
                    var progress = campaignProgressService.progress(orgId, c.getId());
                    return new OrgDashboardResponse.CampaignProgress(
                            c.getId(), c.getName(), c.getStatus(), c.getGoalAmount(),
                            progress.pledged(), progress.collected(), c.getEndDate());
                })
                .toList();

        List<org.donorly.backend.model.Payment> payments =
                paymentRepository.findTop5ByOrganizationIdOrderByCreatedAtDesc(orgId);
        List<org.donorly.backend.model.FollowUp> followUps =
                followUpRepository.findOpenByDueSoonest(orgId, org.springframework.data.domain.PageRequest.of(0, 5));

        // One batched donor lookup for both feeds instead of a query per row (N+1).
        var donorIds = new java.util.HashSet<UUID>();
        payments.forEach(p -> donorIds.add(p.getDonorId()));
        followUps.forEach(f -> donorIds.add(f.getDonorId()));
        var donorNames = new java.util.HashMap<UUID, String>();
        if (!donorIds.isEmpty()) {
            donorRepository.findAllById(donorIds).forEach(d -> donorNames.put(d.getId(), d.getFullName()));
        }

        List<OrgDashboardResponse.RecentPayment> recentPayments = payments.stream()
                .map(p -> new OrgDashboardResponse.RecentPayment(
                        p.getId(), donorNames.getOrDefault(p.getDonorId(), "Donor"),
                        p.getAmount(), p.getPaymentMethod(), p.getPaymentDate()))
                .toList();

        List<OrgDashboardResponse.DueFollowUp> dueFollowUps = followUps.stream()
                .map(f -> new OrgDashboardResponse.DueFollowUp(
                        f.getId(), f.getDonorId(), donorNames.getOrDefault(f.getDonorId(), "Donor"),
                        f.getDueAt(), f.getNotes()))
                .toList();

        return new OrgDashboardResponse(
                totalDonors,
                totalCampaigns,
                pledged,
                collected,
                pledged.subtract(collected),
                openFollowUps,
                outstandingPledges,
                campaigns,
                recentPayments,
                dueFollowUps
        );
    }

    public AmbassadorDashboardResponse ambassadorDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        UUID userId = TenantContext.getUserId();

        // donors assigned to this ambassador
        List<UUID> assignedDonorIds = donorAssignmentRepository
                .findByOrganizationIdAndAmbassadorUserId(orgId, userId).stream()
                .filter(a -> "active".equals(a.getStatus()))
                .map(DonorAssignment::getDonorId)
                .toList();

        long assignedDonors = assignedDonorIds.size();

        // follow-ups assigned to this user
        long totalFollowUps     = followUpRepository.countByOrganizationIdAndAssignedToUserId(orgId, userId);
        long openFollowUps      = followUpRepository.countByOrganizationIdAndAssignedToUserIdAndStatus(orgId, userId, "open");
        long completedFollowUps = followUpRepository.countByOrganizationIdAndAssignedToUserIdAndStatus(orgId, userId, "completed");

        // pledges from assigned donors
        BigDecimal pledged   = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        long pledgeCount     = 0;
        if (!assignedDonorIds.isEmpty()) {
            pledged      = nz(pledgeRepository.sumPledgedByDonors(orgId, assignedDonorIds));
            collected    = nz(pledgeRepository.sumCollectedByDonors(orgId, assignedDonorIds));
            pledgeCount  = pledgeRepository.countByOrganizationIdAndDonorIdIn(orgId, assignedDonorIds);
        }

        // upcoming events — earliest first, from now
        List<AmbassadorDashboardResponse.EventItem> upcomingEvents = eventRepository
                .findByOrganizationIdAndStartsAtGreaterThanEqualOrderByStartsAtAsc(orgId, Instant.now())
                .stream()
                .limit(5)
                .map(e -> new AmbassadorDashboardResponse.EventItem(
                        e.getId(), e.getName(), e.getLocation(), e.getEventType(),
                        e.getStatus(), e.getStartsAt(), e.getEndsAt()))
                .toList();

        // upcoming townhalls — earliest first, from today
        List<AmbassadorDashboardResponse.TownhallItem> upcomingTownhalls = townhallRepository
                .findByOrganizationIdAndEventDateGreaterThanEqualOrderByEventDateAscEventTimeAsc(orgId, LocalDate.now())
                .stream()
                .limit(5)
                .map(t -> new AmbassadorDashboardResponse.TownhallItem(
                        t.getId(), t.getPersonName(), t.getAddress(),
                        t.getEventDate(), t.getEventTime(), t.getDurationMinutes()))
                .toList();

        // active campaigns
        List<AmbassadorDashboardResponse.CampaignItem> activeCampaigns = campaignRepository
                .findByOrganizationIdAndStatusOrderByStartDateAsc(orgId, "active")
                .stream()
                .map(c -> new AmbassadorDashboardResponse.CampaignItem(
                        c.getId(), c.getName(), c.getCampaignType(), c.getStatus(),
                        c.getGoalAmount(), c.getStartDate(), c.getEndDate()))
                .toList();

        return new AmbassadorDashboardResponse(
                assignedDonors, openFollowUps, totalFollowUps, completedFollowUps,
                pledgeCount, pledged, collected, pledged.subtract(collected),
                upcomingEvents, upcomingTownhalls, activeCampaigns
        );
    }

    public CampaignManagerDashboardResponse campaignManagerDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        UUID userId = TenantContext.getUserId();

        // campaigns managed by this user
        List<Campaign> myCampaigns = campaignRepository
                .findByOrganizationIdAndManagedByUserId(orgId, userId);

        List<CampaignManagerDashboardResponse.ManagedCampaign> managedCampaigns = new ArrayList<>();
        BigDecimal totalPledged   = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        long pledgeCount = 0;

        for (Campaign c : myCampaigns) {
            var progress = campaignProgressService.progress(orgId, c.getId());
            BigDecimal p = progress.pledged();
            BigDecimal col = progress.collected();
            long pc = progress.pledgeCount();
            totalPledged   = totalPledged.add(p);
            totalCollected = totalCollected.add(col);
            pledgeCount   += pc;
            managedCampaigns.add(new CampaignManagerDashboardResponse.ManagedCampaign(
                    c.getId(), c.getName(), c.getCampaignType(), c.getStatus(),
                    c.getGoalAmount(), p, col, c.getStartDate(), c.getEndDate()));
        }

        // ambassadors created by this campaign manager
        Role ambassadorRole = roleRepository.findByCode("ambassador").orElse(null);
        List<CampaignManagerDashboardResponse.AmbassadorItem> ambassadors = new ArrayList<>();
        if (ambassadorRole != null) {
            var created = membershipRepository.findByOrganizationIdAndCreatedBy(orgId, userId).stream()
                    .filter(m -> ambassadorRole.getId().equals(m.getRoleId()))
                    .toList();
            // Batch-load users in one query instead of one findById per membership (N+1).
            var usersById = new java.util.HashMap<UUID, org.donorly.backend.model.User>();
            userRepository.findAllById(created.stream().map(m -> m.getUserId()).toList())
                    .forEach(u -> usersById.put(u.getId(), u));
            for (var m : created) {
                var u = usersById.get(m.getUserId());
                if (u != null) {
                    ambassadors.add(new CampaignManagerDashboardResponse.AmbassadorItem(
                            u.getId(), u.getFullName(), u.getEmail(), m.getStatus()));
                }
            }
        }

        // total donors and open follow-ups across my campaigns
        long totalDonors  = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");

        return new CampaignManagerDashboardResponse(
                managedCampaigns, myCampaigns.size(),
                ambassadors,
                totalDonors, openFollowUps,
                pledgeCount, totalPledged, totalCollected,
                totalPledged.subtract(totalCollected)
        );
    }

    public CampaignDashboardResponse campaignDashboard(UUID campaignId) {
        UUID orgId = TenantContext.requireOrganizationId();
        Campaign campaign = campaignRepository.findByIdAndOrganizationId(campaignId, orgId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        var progress = campaignProgressService.progress(orgId, campaignId);
        BigDecimal pledged = progress.pledged();
        BigDecimal collected = progress.collected();
        int pledgeCount = progress.pledgeCount();
        // Live countdown source: days from today until end date, floored at 0.
        Integer daysRemaining = campaign.getEndDate() != null
                ? (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), campaign.getEndDate()))
                : null;
        int donorsTargeted = campaignTargetRepository.resolveTargetedDonorIds(campaignId, orgId).size();
        return new CampaignDashboardResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getGoalAmount(),
                pledged,
                collected,
                campaign.getGoalAmount().subtract(collected),
                pledgeCount,
                campaign.getStatus(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                daysRemaining,
                donorsTargeted
        );
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
