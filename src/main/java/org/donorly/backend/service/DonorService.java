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
        return page(page, size, search, org.donorly.backend.dto.DonorFilter.NONE);
    }

    /**
     * Paginated donor list with server-side filters (group/tag, state, bucket,
     * compliance, giving range, major-donor flag), composed as JPA
     * {@link org.springframework.data.jpa.domain.Specification}s so any subset
     * of filters can be applied in one query.
     */
    public PageResponse<Donor> page(int page, int size, String search,
                                    org.donorly.backend.dto.DonorFilter filter) {
        UUID orgId = TenantContext.requireOrganizationId();
        Pageable pageable = org.donorly.backend.common.PaginationHelper.pageRequest(
                page, size, Sort.by(Sort.Direction.ASC, "fullName"));
        String q = toLikePattern(search);

        Set<UUID> restrictedIds = null;
        if (!canReadAllDonors()) {
            restrictedIds = assignedDonorIds(orgId);
            if (restrictedIds.isEmpty()) {
                return PageResponse.empty(page, size);
            }
        }

        // Legacy fast path (plain JPQL) when no filters are active.
        if (filter == null || filter.isEmpty()) {
            if (restrictedIds == null) {
                return PageResponse.from(donorRepository.pageByOrganization(orgId, q, pageable));
            }
            return PageResponse.from(donorRepository.pageByOrganizationAndIds(orgId, q, restrictedIds, pageable));
        }

        var spec = buildSpec(orgId, q, filter, restrictedIds);
        return PageResponse.from(donorRepository.findAll(spec, pageable));
    }

    private org.springframework.data.jpa.domain.Specification<Donor> buildSpec(
            UUID orgId, String q, org.donorly.backend.dto.DonorFilter filter, Set<UUID> restrictedIds) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("organizationId"), orgId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), q),
                    cb.like(cb.lower(cb.coalesce(root.get("email"), "")), q),
                    cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), q),
                    cb.like(cb.lower(cb.coalesce(root.get("city"), "")), q)));
            if (filter.tagId() != null) {
                var sub = query.subquery(UUID.class);
                var dta = sub.from(org.donorly.backend.model.DonorTagAssignment.class);
                sub.select(dta.get("donorId")).where(cb.equal(dta.get("tagId"), filter.tagId()));
                predicates.add(root.get("id").in(sub));
            }
            if (filter.state() != null && !filter.state().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(cb.coalesce(root.get("state"), "")),
                        filter.state().trim().toLowerCase()));
            }
            if (filter.bucket() != null && !filter.bucket().isBlank()) {
                predicates.add(cb.equal(root.get("bucket"), filter.bucket()));
            }
            if (filter.complianceStatus() != null && !filter.complianceStatus().isBlank()) {
                predicates.add(cb.equal(root.get("complianceStatus"), filter.complianceStatus()));
            }
            if (filter.minAmount() != null) {
                predicates.add(cb.ge(root.get("lifetimeGiving"), filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                predicates.add(cb.le(root.get("lifetimeGiving"), filter.maxAmount()));
            }
            if (filter.majorOnly()) {
                predicates.add(cb.isTrue(root.get("majorDonor")));
            }
            if (restrictedIds != null) {
                predicates.add(root.get("id").in(restrictedIds));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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

    private static final Set<String> BUCKETS = Set.of("confirmed", "potential", "re_registering");
    private static final Set<String> COMPLIANCE = Set.of("ok", "non_compliant", "claims_paid", "non_responsive");

    private void apply(Donor donor, DonorRequest request) {
        donor.setFullName(request.fullName());
        donor.setEmail(blankToNull(request.email()));
        donor.setPhone(blankToNull(request.phone()));
        donor.setCity(blankToNull(request.city()));
        donor.setState(blankToNull(request.state()));
        donor.setAddress(blankToNull(request.address()));
        donor.setAssignedToUserId(request.assignedToUserId());
        if (request.majorDonor() != null) {
            donor.setMajorDonor(request.majorDonor());
        }
        if (request.bucket() != null && BUCKETS.contains(request.bucket())) {
            donor.setBucket(request.bucket());
        }
        if (request.complianceStatus() != null && COMPLIANCE.contains(request.complianceStatus())) {
            donor.setComplianceStatus(request.complianceStatus());
        }
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
