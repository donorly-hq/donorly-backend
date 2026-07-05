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
    private final org.donorly.backend.repository.ReceiptRepository receiptRepository;

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

    /** One statement per donor covering all payments in {@code year}, for tax letters. */
    public org.donorly.backend.dto.YearEndStatementResponse yearEndStatements(int year) {
        UUID orgId = TenantContext.requireOrganizationId();

        var yearPayments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().getYear() == year)
                .toList();

        var receiptsByPayment = new java.util.HashMap<UUID, String>();
        for (Payment p : yearPayments) {
            receiptRepository.findByPaymentId(p.getId())
                    .ifPresent(r -> receiptsByPayment.put(p.getId(), r.getReceiptNumber()));
        }

        var donors = donorRepository.findAllById(
                yearPayments.stream().map(Payment::getDonorId).collect(java.util.stream.Collectors.toSet()));
        var donorById = donors.stream()
                .collect(java.util.stream.Collectors.toMap(org.donorly.backend.model.Donor::getId, d -> d));

        var byDonor = yearPayments.stream()
                .collect(java.util.stream.Collectors.groupingBy(Payment::getDonorId));

        var statements = byDonor.entrySet().stream()
                .map(entry -> {
                    var donor = donorById.get(entry.getKey());
                    var lines = entry.getValue().stream()
                            .sorted(java.util.Comparator.comparing(Payment::getPaymentDate))
                            .map(p -> new org.donorly.backend.dto.YearEndStatementResponse.PaymentLine(
                                    p.getPaymentDate(), p.getAmount(), p.getPaymentMethod(),
                                    receiptsByPayment.get(p.getId())))
                            .toList();
                    BigDecimal total = entry.getValue().stream()
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new org.donorly.backend.dto.YearEndStatementResponse.DonorStatement(
                            entry.getKey(),
                            donor != null ? donor.getFullName() : "Unknown donor",
                            donor != null ? donor.getEmail() : null,
                            donor != null ? donor.getCity() : null,
                            total, lines);
                })
                .sorted(java.util.Comparator.comparing(
                        org.donorly.backend.dto.YearEndStatementResponse.DonorStatement::donorName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new org.donorly.backend.dto.YearEndStatementResponse(year, statements);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
