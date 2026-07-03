package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.dto.DonorCreateRequest;
import org.donorly.donorly_backend.dto.DonorResponseDto;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.model.Pledge;
import org.donorly.donorly_backend.repository.DonorRepository;
import org.donorly.donorly_backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;

    // Default tenant/campaign seeded by V4 migration — used until real
    // multi-tenant JWT resolution is wired up.
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEFAULT_CAMPAIGN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public List<DonorResponseDto> getAll() {
        return donorRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<DonorResponseDto> getById(UUID id) {
        return donorRepository.findById(id).map(this::toDto);
    }

    public List<DonorResponseDto> getByAmbassadorId(UUID ambassadorId) {
        List<UUID> donorIds = pledgeRepository.findByAmbassadorId(ambassadorId)
                .stream().map(Pledge::getDonorId).distinct().collect(Collectors.toList());
        return donorRepository.findAllById(donorIds).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public DonorResponseDto create(DonorCreateRequest req) {
        String[] nameParts = (req.name == null ? "" : req.name.trim()).split("\\s+", 2);
        String firstName = nameParts.length > 0 && !nameParts[0].isBlank() ? nameParts[0] : "Donor";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Donor donor = new Donor();
        donor.setTenantId(DEFAULT_TENANT_ID);
        donor.setFirstName(firstName);
        donor.setLastName(lastName);
        donor.setPhoneNumber(req.phone);
        donor.setEmailAddress(req.email);
        donor.setDonorType("individual");
        Donor savedDonor = donorRepository.save(donor);

        Pledge pledge = new Pledge();
        pledge.setTenantId(DEFAULT_TENANT_ID);
        pledge.setCampaignId(DEFAULT_CAMPAIGN_ID);
        pledge.setDonorId(savedDonor.getDonorId());
        pledge.setAmbassadorId(req.ambassadorId);
        pledge.setPledgeNumber("PLG-" + System.currentTimeMillis());
        pledge.setPledgedAmount(req.pledgeAmount == null ? BigDecimal.ZERO : req.pledgeAmount);
        pledge.setRecurrenceFrequency(mapRecurrence(req.recurringType));
        pledge.setPledgeStatus(mapStatusToPledgeStatus(req.status));
        pledge.setDonationType(req.donationType);
        pledge.setCorporateMatch(req.corporateMatch);
        pledge.setEmployerName(req.employer);
        pledge.setPreferredPaymentMethod(req.paymentMethod);
        pledgeRepository.save(pledge);

        return toDto(savedDonor);
    }

    @Transactional
    public DonorResponseDto updateStatus(UUID donorId, String newStatus) {
        List<Pledge> pledges = pledgeRepository.findAll().stream()
                .filter(p -> p.getDonorId() != null && p.getDonorId().equals(donorId))
                .collect(Collectors.toList());
        if (!pledges.isEmpty()) {
            Pledge p = pledges.get(0);
            p.setPledgeStatus(mapStatusToPledgeStatus(newStatus));
            pledgeRepository.save(p);
        }
        return donorRepository.findById(donorId).map(this::toDto).orElse(null);
    }

    public void delete(UUID id) {
        donorRepository.deleteById(id);
    }

    private String mapRecurrence(String frontendValue) {
        if (frontendValue == null) return "one_time";
        return switch (frontendValue) {
            case "monthly" -> "monthly";
            case "quarterly" -> "quarterly";
            case "annually", "yearly" -> "yearly";
            default -> "one_time";
        };
    }

    private String mapStatusToPledgeStatus(String frontendStatus) {
        return "Collected".equals(frontendStatus) ? "paid" : "committed";
    }

    private DonorResponseDto toDto(Donor donor) {
        DonorResponseDto dto = new DonorResponseDto();
        dto.id = donor.getDonorId();
        dto.name = (donor.getFirstName() == null ? "" : donor.getFirstName())
                + (donor.getLastName() == null || donor.getLastName().isBlank() ? "" : " " + donor.getLastName());
        dto.phone = donor.getPhoneNumber();
        dto.email = donor.getEmailAddress();
        dto.address = null;
        dto.createdAt = donor.getCreatedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(donor.getCreatedAt());

        List<Pledge> pledges = pledgeRepository.findAll().stream()
                .filter(p -> p.getDonorId() != null && p.getDonorId().equals(donor.getDonorId()))
                .collect(Collectors.toList());

        if (!pledges.isEmpty()) {
            Pledge p = pledges.get(0);
            dto.pledgeAmount = p.getPledgedAmount();
            dto.donationType = p.getDonationType() == null ? "General" : p.getDonationType();
            dto.recurringType = p.getRecurrenceFrequency();
            dto.duration = 1;
            dto.paymentMethod = p.getPreferredPaymentMethod();
            dto.ambassadorId = p.getAmbassadorId();
            dto.corporateMatch = p.isCorporateMatch();
            dto.employer = p.getEmployerName();
            dto.status = "paid".equals(p.getPledgeStatus()) ? "Collected" : "Pledged";
        } else {
            dto.pledgeAmount = BigDecimal.ZERO;
            dto.donationType = "General";
            dto.recurringType = "one_time";
            dto.duration = 1;
            dto.status = "Pledged";
            dto.corporateMatch = false;
        }

        return dto;
    }
}
