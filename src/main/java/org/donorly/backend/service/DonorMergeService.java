package org.donorly.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorMergeRequest;
import org.donorly.backend.dto.DuplicateGroupResponse;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Duplicate donor detection and merge.
 *
 * Duplicate emails cannot exist (unique index per org), so detection clusters
 * by normalized phone number and by normalized full name.
 */
@Service
@RequiredArgsConstructor
public class DonorMergeService {

    /** Tables where donor_id can simply be repointed to the surviving donor. */
    private static final List<String> SIMPLE_REASSIGN_TABLES = List.of(
            "pledges", "payments", "follow_ups", "donor_notes",
            "pledge_cards", "event_registrations", "communication_messages");

    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<DuplicateGroupResponse> findDuplicates() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<Donor> donors = donorRepository.findByOrganizationIdAndDeletedAtIsNull(orgId);

        List<DuplicateGroupResponse> groups = new ArrayList<>();
        Set<Set<UUID>> seenGroups = new HashSet<>();

        collectGroups(donors, groups, seenGroups, "Same phone number",
                d -> DonorImportService.normalizePhone(d.getPhone()), 7);
        collectGroups(donors, groups, seenGroups, "Similar name",
                d -> DonorImportService.normalizeName(d.getFullName()), 3);
        return groups;
    }

    private void collectGroups(List<Donor> donors, List<DuplicateGroupResponse> groups,
                               Set<Set<UUID>> seenGroups, String reason,
                               java.util.function.Function<Donor, String> keyFn, int minKeyLength) {
        Map<String, List<Donor>> byKey = new LinkedHashMap<>();
        for (Donor d : donors) {
            String key = keyFn.apply(d);
            if (key == null || key.length() < minKeyLength) continue;
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        for (List<Donor> cluster : byKey.values()) {
            if (cluster.size() < 2) continue;
            Set<UUID> ids = new HashSet<>(cluster.stream().map(Donor::getId).toList());
            if (seenGroups.add(ids)) {
                groups.add(new DuplicateGroupResponse(reason, cluster));
            }
        }
    }

    @Transactional
    public Donor merge(DonorMergeRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        Donor keeper = donorRepository.findByIdAndOrganizationId(request.keepId(), orgId)
                .filter(d -> d.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Donor to keep not found"));

        for (UUID mergeId : request.mergeIds()) {
            if (mergeId.equals(keeper.getId())) {
                throw new BadRequestException("Cannot merge a donor into itself");
            }
            Donor duplicate = donorRepository.findByIdAndOrganizationId(mergeId, orgId)
                    .filter(d -> d.getDeletedAt() == null)
                    .orElseThrow(() -> new NotFoundException("Donor to merge not found: " + mergeId));
            mergeOne(orgId, keeper, duplicate);
        }

        recomputeLifetimeGiving(orgId, keeper);
        auditService.record("donor.merge", "donor", keeper.getId());
        return donorRepository.findByIdAndOrganizationId(keeper.getId(), orgId).orElseThrow();
    }

    private void mergeOne(UUID orgId, Donor keeper, Donor duplicate) {
        UUID keepId = keeper.getId();
        UUID dupId = duplicate.getId();

        for (String table : SIMPLE_REASSIGN_TABLES) {
            entityManager.createNativeQuery(
                            "UPDATE " + table + " SET donor_id = :keep WHERE donor_id = :dup AND organization_id = :org")
                    .setParameter("keep", keepId).setParameter("dup", dupId).setParameter("org", orgId)
                    .executeUpdate();
        }

        // Tag assignments: PK (donor_id, tag_id) — drop overlaps before repointing
        entityManager.createNativeQuery("""
                        DELETE FROM donor_tag_assignments d
                        WHERE d.donor_id = :dup
                          AND EXISTS (SELECT 1 FROM donor_tag_assignments k
                                      WHERE k.donor_id = :keep AND k.tag_id = d.tag_id)
                        """)
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();
        entityManager.createNativeQuery("UPDATE donor_tag_assignments SET donor_id = :keep WHERE donor_id = :dup")
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();

        // Ambassador assignments: UNIQUE (donor_id, campaign_id, ambassador_user_id)
        entityManager.createNativeQuery("""
                        DELETE FROM donor_assignments d
                        WHERE d.donor_id = :dup
                          AND EXISTS (SELECT 1 FROM donor_assignments k
                                      WHERE k.donor_id = :keep
                                        AND k.ambassador_user_id = d.ambassador_user_id
                                        AND k.campaign_id IS NOT DISTINCT FROM d.campaign_id)
                        """)
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();
        entityManager.createNativeQuery("UPDATE donor_assignments SET donor_id = :keep WHERE donor_id = :dup")
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();

        // Profile: PK donor_id — keep the survivor's profile when both exist
        entityManager.createNativeQuery("""
                        DELETE FROM donor_profiles d
                        WHERE d.donor_id = :dup
                          AND EXISTS (SELECT 1 FROM donor_profiles k WHERE k.donor_id = :keep)
                        """)
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();
        entityManager.createNativeQuery("UPDATE donor_profiles SET donor_id = :keep WHERE donor_id = :dup")
                .setParameter("keep", keepId).setParameter("dup", dupId).executeUpdate();

        // Fill missing contact fields from the duplicate. The org+email unique index
        // includes soft-deleted rows, so free the email on the duplicate first.
        String dupEmail = duplicate.getEmail();
        String dupPhone = duplicate.getPhone();
        String dupCity = duplicate.getCity();

        duplicate.setEmail(null);
        duplicate.setStatus("merged");
        duplicate.setDeletedAt(Instant.now());
        donorRepository.saveAndFlush(duplicate);

        if (keeper.getEmail() == null && dupEmail != null) keeper.setEmail(dupEmail);
        if (keeper.getPhone() == null && dupPhone != null) keeper.setPhone(dupPhone);
        if (keeper.getCity() == null && dupCity != null) keeper.setCity(dupCity);
        donorRepository.save(keeper);
    }

    private void recomputeLifetimeGiving(UUID orgId, Donor keeper) {
        BigDecimal collected = pledgeRepository.findByOrganizationIdAndDonorId(orgId, keeper.getId()).stream()
                .map(Pledge::getCollectedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        keeper.setLifetimeGiving(collected);
        donorRepository.save(keeper);
    }
}
