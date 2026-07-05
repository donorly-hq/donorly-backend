package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.DonorImportRequest;
import org.donorly.backend.dto.DonorImportResult;
import org.donorly.backend.model.Donor;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Bulk donor import for spreadsheet/CRM migrations. Duplicate rows are skipped, not rejected. */
@Service
@RequiredArgsConstructor
public class DonorImportService {

    private static final Set<String> DONOR_TYPES = Set.of("individual", "family", "business", "anonymous");

    private final DonorRepository donorRepository;
    private final AuditService auditService;

    @Transactional
    public DonorImportResult importDonors(DonorImportRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<Donor> existing = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);

        Set<String> knownEmails = new HashSet<>();
        Set<String> knownNamePhones = new HashSet<>();
        for (Donor d : existing) {
            if (d.getEmail() != null) knownEmails.add(d.getEmail().toLowerCase(Locale.ROOT));
            knownNamePhones.add(namePhoneKey(d.getFullName(), d.getPhone()));
        }

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        int rowNumber = 0;
        for (DonorImportRequest.Row row : request.donors()) {
            rowNumber++;
            String name = trimToNull(row.fullName());
            if (name == null) {
                errors.add("Row " + rowNumber + ": full name is required");
                continue;
            }
            String email = trimToNull(row.email());
            String emailKey = email != null ? email.toLowerCase(Locale.ROOT) : null;
            String phone = trimToNull(row.phone());

            if ((emailKey != null && knownEmails.contains(emailKey))
                    || knownNamePhones.contains(namePhoneKey(name, phone))) {
                skipped++;
                continue;
            }

            Donor donor = new Donor();
            donor.setOrganizationId(orgId);
            donor.setFullName(name);
            donor.setEmail(email);
            donor.setPhone(phone);
            donor.setCity(trimToNull(row.city()));
            String type = trimToNull(row.donorType());
            donor.setDonorType(type != null && DONOR_TYPES.contains(type.toLowerCase(Locale.ROOT))
                    ? type.toLowerCase(Locale.ROOT) : "individual");
            donorRepository.save(donor);

            if (emailKey != null) knownEmails.add(emailKey);
            knownNamePhones.add(namePhoneKey(name, phone));
            imported++;
        }

        auditService.record("donor.import", "donor", null);
        return new DonorImportResult(imported, skipped, errors);
    }

    private static String namePhoneKey(String name, String phone) {
        return normalizeName(name) + "|" + normalizePhone(phone);
    }

    static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
