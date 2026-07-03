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
    private final EventRepository eventRepository;
    private final TownhallRepository townhallRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OrgDashboardResponse orgDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        long totalDonors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long totalCampaigns = campaignRepository.findByOrganizationId(orgId).size();
        BigDecimal pledged = nz(pledgeRepository.sumPledgedByOrganization(orgId));
        BigDecimal collected = nz(pledgeRepository.sumCollectedByOrganization(orgId));
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");
        return new OrgDashboardResponse(
                totalDonors,
                totalCampaigns,
                pledged,
                collected,
                pledged.subtract(collected),
                openFollowUps
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
            BigDecimal p = nz(pledgeRepository.sumPledgedByCampaign(orgId, c.getId()));
            BigDecimal col = nz(pledgeRepository.sumCollectedByCampaign(orgId, c.getId()));
            long pc = pledgeRepository.findByOrganizationIdAndCampaignId(orgId, c.getId()).size();
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
            for (var m : membershipRepository.findByOrganizationIdAndCreatedBy(orgId, userId)) {
                if (!ambassadorRole.getId().equals(m.getRoleId())) continue;
                userRepository.findById(m.getUserId()).ifPresent(u ->
                        ambassadors.add(new CampaignManagerDashboardResponse.AmbassadorItem(
                                u.getId(), u.getFullName(), u.getEmail(), m.getStatus())));
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
        BigDecimal pledged = nz(pledgeRepository.sumPledgedByCampaign(orgId, campaignId));
        BigDecimal collected = nz(pledgeRepository.sumCollectedByCampaign(orgId, campaignId));
        int pledgeCount = pledgeRepository.findByOrganizationIdAndCampaignId(orgId, campaignId).size();
        return new CampaignDashboardResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getGoalAmount(),
                pledged,
                collected,
                campaign.getGoalAmount().subtract(collected),
                pledgeCount
        );
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
