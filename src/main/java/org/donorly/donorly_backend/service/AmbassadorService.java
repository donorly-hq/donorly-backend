package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.dto.AmbassadorCreateRequest;
import org.donorly.donorly_backend.dto.AmbassadorResponseDto;
import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.model.AppUser;
import org.donorly.donorly_backend.model.Pledge;
import org.donorly.donorly_backend.repository.AmbassadorRepository;
import org.donorly.donorly_backend.repository.AppUserRepository;
import org.donorly.donorly_backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * KNOWN GAP: the old version of handoverTo() also transferred
 * PledgeCard records by ambassadorId. The new schema's pledge_cards
 * table has no ambassador_id column, so that step is left out here.
 * If you need pledge cards to follow the handover too, we need to
 * either add an ambassador_id column to pledge_cards, or derive
 * ownership via the pledge_cards -> donor -> ambassador_donor_assignments
 * chain instead. Flagging rather than guessing.
 */
@Service
@RequiredArgsConstructor
public class AmbassadorService {

    private final AmbassadorRepository ambassadorRepository;
    private final PledgeRepository pledgeRepository;
    private final AppUserRepository appUserRepository;

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public List<AmbassadorResponseDto> getAllDto() {
        return ambassadorRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<AmbassadorResponseDto> getByIdDto(UUID id) {
        return ambassadorRepository.findById(id).map(this::toDto);
    }

    public List<AmbassadorResponseDto> getDownlineDto(UUID ambassadorId) {
        return ambassadorRepository.findByAncestorPathContains(ambassadorId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AmbassadorResponseDto createRootFromRequest(AmbassadorCreateRequest req) {
        Ambassador a = fromRequest(req);
        a.setAncestorPath(new ArrayList<>());
        a.setParentAmbassadorId(null);
        return toDto(ambassadorRepository.save(a));
    }

    @Transactional
    public AmbassadorResponseDto createSubFromRequest(UUID parentId, AmbassadorCreateRequest req) {
        Ambassador parent = ambassadorRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent ambassador not found"));
        Ambassador a = fromRequest(req);
        List<UUID> ancestorPath = new ArrayList<>(
                parent.getAncestorPath() == null ? List.of() : parent.getAncestorPath());
        ancestorPath.add(parentId);
        a.setParentAmbassadorId(parentId);
        a.setAncestorPath(ancestorPath);
        return toDto(ambassadorRepository.save(a));
    }

    private Ambassador fromRequest(AmbassadorCreateRequest req) {
        Ambassador a = new Ambassador();
        a.setTenantId(DEFAULT_TENANT_ID);
        a.setDisplayName(req.name);
        a.setEmailAddress(req.email);
        a.setPhoneNumber(req.phone);
        a.setAmbassadorCode(req.code);
        a.setPledgeGoal(req.pledgeGoal == null ? BigDecimal.valueOf(10000) : req.pledgeGoal);
        a.setActive(true);
        return a;
    }

    private AmbassadorResponseDto toDto(Ambassador a) {
        AmbassadorResponseDto dto = new AmbassadorResponseDto();
        dto.id = a.getAmbassadorId();
        dto.name = a.getDisplayName();
        dto.email = a.getEmailAddress();
        dto.phone = a.getPhoneNumber();
        dto.city = null;
        dto.code = a.getAmbassadorCode();
        dto.pledgeGoal = a.getPledgeGoal();
        dto.totalPledged = pledgeRepository.sumPledgedAmountByAmbassadorId(a.getAmbassadorId());
        dto.status = a.isActive() ? "Active" : "Inactive";
        dto.createdAt = a.getCreatedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(a.getCreatedAt());
        dto.parentAmbassadorId = a.getParentAmbassadorId();
        return dto;
    }

    public List<Ambassador> getAll() {
        return ambassadorRepository.findAll();
    }

    public Optional<Ambassador> getById(UUID id) {
        return ambassadorRepository.findById(id);
    }

    public Ambassador save(Ambassador ambassador) {
        return ambassadorRepository.save(ambassador);
    }

    public Ambassador createRootAmbassador(Ambassador ambassador) {
        ambassador.setAncestorPath(new ArrayList<>());
        ambassador.setParentAmbassadorId(null);
        return ambassadorRepository.save(ambassador);
    }

    public Ambassador createSubAmbassador(UUID parentId, Ambassador ambassador) {
        Ambassador parent = ambassadorRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent ambassador not found"));

        List<UUID> ancestorPath = new ArrayList<>(
                parent.getAncestorPath() == null ? List.of() : parent.getAncestorPath());
        ancestorPath.add(parentId);

        ambassador.setParentAmbassadorId(parentId);
        ambassador.setAncestorPath(ancestorPath);
        return ambassadorRepository.save(ambassador);
    }

    public List<Ambassador> getDownline(UUID ambassadorId) {
        return ambassadorRepository.findByAncestorPathContains(ambassadorId);
    }

    public boolean isSelfOrAncestor(UUID ambassadorId, UUID targetId) {
        if (ambassadorId.equals(targetId)) return true;
        return ambassadorRepository.findById(targetId)
                .map(a -> a.getAncestorPath() != null && a.getAncestorPath().contains(ambassadorId))
                .orElse(false);
    }

    public Ambassador deactivate(UUID id) {
        Ambassador ambassador = ambassadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ambassador not found"));
        ambassador.setActive(false);
        return ambassadorRepository.save(ambassador);
    }

    @Transactional
    public void handoverTo(UUID fromId, UUID toId) {
        Ambassador from = ambassadorRepository.findById(fromId)
                .orElseThrow(() -> new RuntimeException("From ambassador not found"));
        Ambassador to = ambassadorRepository.findById(toId)
                .orElseThrow(() -> new RuntimeException("To ambassador not found"));

        // Transfer pledges (replaces the old "transfer donors" step —
        // donor <-> ambassador linkage now lives on Pledge, not Donor)
        List<Pledge> pledges = pledgeRepository.findByAmbassadorId(fromId);
        pledges.forEach(p -> p.setAmbassadorId(toId));
        pledgeRepository.saveAll(pledges);

        // NOTE: totalPledged is no longer stored on Ambassador — query
        // pledgeRepository.sumPledgedAmountByAmbassadorId(toId) when you
        // need the current total instead of trusting a cached field.

        // Transfer hierarchy position
        to.setParentAmbassadorId(from.getParentAmbassadorId());
        to.setAncestorPath(new ArrayList<>(
                from.getAncestorPath() == null ? List.of() : from.getAncestorPath()));
        ambassadorRepository.save(to);

        // Update all of 'to's pre-existing descendants' ancestorPath
        List<Ambassador> toDescendants = ambassadorRepository.findByAncestorPathContains(toId);
        toDescendants.forEach(d -> {
            List<UUID> path = new ArrayList<>(d.getAncestorPath());
            int idx = path.indexOf(fromId);
            if (idx >= 0) path.set(idx, toId);
            d.setAncestorPath(path);
        });
        ambassadorRepository.saveAll(toDescendants);

        // Update from's former subtree
        List<Ambassador> fromDescendants = ambassadorRepository.findByAncestorPathContains(fromId);
        fromDescendants.forEach(d -> {
            List<UUID> path = new ArrayList<>(d.getAncestorPath());
            path.replaceAll(id -> id.equals(fromId) ? toId : id);
            d.setAncestorPath(path);
            if (fromId.equals(d.getParentAmbassadorId())) {
                d.setParentAmbassadorId(toId);
            }
        });
        ambassadorRepository.saveAll(fromDescendants);

        // Deactivate 'from'
        from.setActive(false);
        from.setParentAmbassadorId(null);
        from.setAncestorPath(new ArrayList<>());
        ambassadorRepository.save(from);

        // Lock 'from' user login
        appUserRepository.findByEmailAddress(from.getEmailAddress()).ifPresent(user -> {
            user.setUserStatus("disabled");
            user.setActiveSessionToken(null);
            appUserRepository.save(user);
        });
    }
}
