package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.OrgUsageMetrics;
import org.donorly.backend.model.Organization;
import org.donorly.backend.repository.AuditLogRepository;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationMembershipRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Usage/health snapshots per tenant for the platform admin console. */
@Service
@RequiredArgsConstructor
public class PlatformMetricsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final AuditLogRepository auditLogRepository;

    public List<OrgUsageMetrics> listUsageMetrics() {
        return organizationRepository.findAll().stream()
                .filter(o -> o.getDeletedAt() == null)
                .map(this::metricsFor)
                .toList();
    }

    private OrgUsageMetrics metricsFor(Organization org) {
        UUID orgId = org.getId();
        return new OrgUsageMetrics(
                orgId,
                membershipRepository.countByOrganizationIdAndStatus(orgId, "active"),
                donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId),
                campaignRepository.countByOrganizationIdAndStatus(orgId, "active"),
                pledgeRepository.countByOrganizationId(orgId),
                pledgeRepository.sumPledgedByOrganization(orgId),
                pledgeRepository.sumCollectedByOrganization(orgId),
                auditLogRepository.findLastActivityAt(orgId)
        );
    }
}
