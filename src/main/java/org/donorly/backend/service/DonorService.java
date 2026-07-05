package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.common.Permissions;
import org.donorly.backend.dto.DonorRequest;
import org.donorly.backend.dto.PageResponse;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorAssignment;
import org.donorly.backend.repository.DonorAssignmentRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.security.SecurityUtils;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final DonorAssignmentRepository assignmentRepository;
    private final AuditService auditService;

    public List<Donor> list() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<Donor> donors = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);
        if (canReadAllDonors()) {
            return donors;
        }
        Set<UUID> assignedDonorIds = assignedDonorIds(orgId);
        return donors.stream()
                .filter(d -> assignedDonorIds.contains(d.getId()))
                .collect(Collectors.toList());
    }

    /** Paginated, optionally-searched donor list. Ambassadors only see assigned donors. */
    public PageResponse<Donor> page(int page, int size, String search) {
        UUID orgId = TenantContext.requireOrganizationId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.ASC, "fullName"));
        String q = toLikePattern(search);
        if (canReadAllDonors()) {
            return PageResponse.from(donorRepository.pageByOrganization(orgId, q, pageable));
        }
        Set<UUID> assignedDonorIds = assignedDonorIds(orgId);
        if (assignedDonorIds.isEmpty()) {
            return PageResponse.empty(page, size);
        }
        return PageResponse.from(donorRepository.pageByOrganizationAndIds(orgId, q, assignedDonorIds, pageable));
    }

    static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 200);
    }

    static String toLikePattern(String search) {
        return (search == null || search.isBlank()) ? "%" : "%" + search.trim().toLowerCase() + "%";
    }

    public Donor get(UUID id) {
        Donor donor = donorRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Donor not found"));
        if (!canReadAllDonors() && !assignedDonorIds(donor.getOrganizationId()).contains(donor.getId())) {
            throw new NotFoundException("Donor not found");
        }
        return donor;
    }

    private boolean canReadAllDonors() {
        return SecurityUtils.hasAuthority(Permissions.DONORS_READ_ALL);
    }

    private Set<UUID> assignedDonorIds(UUID orgId) {
        UUID userId = TenantContext.getUserId();
        return assignmentRepository.findByOrganizationIdAndAmbassadorUserId(orgId, userId).stream()
                .filter(a -> "active".equals(a.getStatus()))
                .map(DonorAssignment::getDonorId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public Donor create(DonorRequest request) {
        Donor donor = new Donor();
        donor.setOrganizationId(TenantContext.requireOrganizationId());
        apply(donor, request);
        Donor saved = donorRepository.save(donor);
        auditService.record("donor.create", "donor", saved.getId());
        return saved;
    }

    @Transactional
    public Donor update(UUID id, DonorRequest request) {
        Donor donor = get(id);
        apply(donor, request);
        Donor saved = donorRepository.save(donor);
        auditService.record("donor.update", "donor", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Donor donor = get(id);
        donor.setDeletedAt(Instant.now());
        donorRepository.save(donor);
        auditService.record("donor.delete", "donor", id);
    }

    private void apply(Donor donor, DonorRequest request) {
        donor.setFullName(request.fullName());
        donor.setEmail(blankToNull(request.email()));
        donor.setPhone(blankToNull(request.phone()));
        donor.setCity(blankToNull(request.city()));
        if (request.donorType() != null && !request.donorType().isBlank()) {
            donor.setDonorType(request.donorType());
        }
        if (request.status() != null && !request.status().isBlank()) {
            donor.setStatus(request.status());
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
