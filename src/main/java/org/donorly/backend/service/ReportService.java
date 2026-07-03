package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.FundraisingReportResponse;
import org.donorly.backend.model.Payment;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.PaymentRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final FollowUpRepository followUpRepository;
    private final PaymentRepository paymentRepository;

    public FundraisingReportResponse fundraisingReport() {
        UUID orgId = TenantContext.requireOrganizationId();

        long totalDonors = donorRepository.countByOrganizationIdAndDeletedAtIsNull(orgId);
        long activeCampaigns = campaignRepository.findByOrganizationId(orgId).stream()
                .filter(c -> "active".equals(c.getStatus()))
                .count();

        BigDecimal totalPledged = nz(pledgeRepository.sumPledgedByOrganization(orgId));
        BigDecimal totalCollected = nz(pledgeRepository.sumCollectedByOrganization(orgId));

        var pledges = pledgeRepository.findByOrganizationId(orgId);
        long fulfilledPledges = pledges.stream().filter(p -> "fulfilled".equals(p.getStatus())).count();
        long openFollowUps = followUpRepository.countByOrganizationIdAndStatus(orgId, "open");

        YearMonth thisMonth = YearMonth.now();
        LocalDate monthStart = thisMonth.atDay(1);
        LocalDate monthEnd = thisMonth.atEndOfMonth();

        var monthPayments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(p -> !p.getPaymentDate().isBefore(monthStart) && !p.getPaymentDate().isAfter(monthEnd))
                .toList();

        BigDecimal collectedThisMonth = monthPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FundraisingReportResponse(
                totalDonors,
                activeCampaigns,
                totalPledged,
                totalCollected,
                totalPledged.subtract(totalCollected),
                pledges.size(),
                fulfilledPledges,
                openFollowUps,
                monthPayments.size(),
                collectedThisMonth
        );
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
