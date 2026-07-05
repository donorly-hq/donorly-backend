package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PaymentResponse;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Builds CSV exports for the spreadsheet-centric workflows (boards, treasurers). */
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;
    private final CampaignRepository campaignRepository;
    private final PaymentService paymentService;

    public String donorsCsv() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<Donor> donors = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Full name", "Email", "Phone", "City", "Type", "Status", "Lifetime giving", "Created at");
        for (Donor d : donors) {
            appendRow(sb, d.getFullName(), d.getEmail(), d.getPhone(), d.getCity(),
                    d.getDonorType(), d.getStatus(),
                    d.getLifetimeGiving() != null ? d.getLifetimeGiving().toPlainString() : "0",
                    d.getCreatedAt() != null ? d.getCreatedAt().toString() : "");
        }
        return sb.toString();
    }

    public String paymentsCsv() {
        List<PaymentResponse> payments = paymentService.list();
        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Date", "Donor", "Amount", "Method", "Reference", "Receipt number", "Notes");
        for (PaymentResponse p : payments) {
            appendRow(sb,
                    p.paymentDate() != null ? p.paymentDate().toString() : "",
                    p.donorName(),
                    p.amount() != null ? p.amount().toPlainString() : "0",
                    p.paymentMethod(),
                    p.reference(),
                    p.receipt() != null ? p.receipt().receiptNumber() : "",
                    p.notes());
        }
        return sb.toString();
    }

    public String pledgesCsv() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<Pledge> pledges = pledgeRepository.findByOrganizationId(orgId);

        Map<UUID, String> donorNames = donorRepository
                .findAllById(pledges.stream().map(Pledge::getDonorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Donor::getId, Donor::getFullName));
        Map<UUID, String> campaignNames = campaignRepository
                .findAllById(pledges.stream().map(Pledge::getCampaignId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Campaign::getId, Campaign::getName));

        StringBuilder sb = new StringBuilder();
        appendRow(sb, "Donor", "Campaign", "Amount", "Collected", "Frequency", "Status", "Start date", "Payment method");
        for (Pledge p : pledges) {
            appendRow(sb,
                    donorNames.getOrDefault(p.getDonorId(), ""),
                    campaignNames.getOrDefault(p.getCampaignId(), ""),
                    p.getAmount() != null ? p.getAmount().toPlainString() : "0",
                    p.getCollectedAmount() != null ? p.getCollectedAmount().toPlainString() : "0",
                    p.getFrequency(), p.getStatus(),
                    p.getStartDate() != null ? p.getStartDate().toString() : "",
                    p.getPaymentMethod());
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, String... cells) {
        sb.append(java.util.Arrays.stream(cells).map(CsvExportService::escape)
                .collect(Collectors.joining(","))).append("\r\n");
    }

    private static String escape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
