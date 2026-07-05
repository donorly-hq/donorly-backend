package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.InventoryAssignRequest;
import org.donorly.backend.dto.InventoryItemRequest;
import org.donorly.backend.dto.InventoryItemResponse;
import org.donorly.backend.dto.InventoryUnitStatus;
import org.donorly.backend.model.InventoryAssignment;
import org.donorly.backend.model.InventoryItem;
import org.donorly.backend.model.User;
import org.donorly.backend.repository.InventoryAssignmentRepository;
import org.donorly.backend.repository.InventoryItemRepository;
import org.donorly.backend.repository.UserRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Org-defined inventory (Square readers, standees, banners...) with per-unit
 * accountability: each physical unit is checked out to a holder and checked
 * back in, so "who has standee #3 and since when" is always answerable.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<InventoryItemResponse> list() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<InventoryItem> items = itemRepository.findByOrganizationIdOrderByNameAsc(orgId);
        List<InventoryAssignment> active = assignmentRepository.findByOrganizationIdAndReturnedAtIsNull(orgId);

        Map<UUID, List<InventoryAssignment>> byItem = active.stream()
                .collect(Collectors.groupingBy(InventoryAssignment::getItemId));

        Map<UUID, String> holderNames = userRepository.findAllById(
                        active.stream().map(InventoryAssignment::getHolderUserId)
                                .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        return items.stream()
                .map(item -> toResponse(item, byItem.getOrDefault(item.getId(), List.of()), holderNames))
                .toList();
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        InventoryItem item = new InventoryItem();
        item.setOrganizationId(orgId);
        applyRequest(item, request);
        InventoryItem saved = itemRepository.save(item);
        auditService.record("inventory.item_create", "inventory_item", saved.getId());
        return toResponse(saved, List.of(), Map.of());
    }

    @Transactional
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        InventoryItem item = itemRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        List<InventoryAssignment> active = assignmentRepository.findByItemIdAndReturnedAtIsNull(id);
        int highestUnitOut = active.stream().mapToInt(InventoryAssignment::getUnitNumber).max().orElse(0);
        if (request.quantity() < highestUnitOut) {
            throw new BadRequestException(
                    "Unit #" + highestUnitOut + " is still checked out — collect it before reducing the quantity");
        }

        applyRequest(item, request);
        InventoryItem saved = itemRepository.save(item);
        auditService.record("inventory.item_update", "inventory_item", saved.getId());
        return toResponse(saved, active, resolveHolderNames(active));
    }

    @Transactional
    public void delete(UUID id) {
        UUID orgId = TenantContext.requireOrganizationId();
        InventoryItem item = itemRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));
        if (!assignmentRepository.findByItemIdAndReturnedAtIsNull(id).isEmpty()) {
            throw new BadRequestException("Units are still checked out — collect them before deleting this item");
        }
        itemRepository.delete(item);
        auditService.record("inventory.item_delete", "inventory_item", id);
    }

    @Transactional
    public InventoryItemResponse assign(UUID itemId, InventoryAssignRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        InventoryItem item = itemRepository.findByIdAndOrganizationId(itemId, orgId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        if (request.unitNumber() > item.getQuantity()) {
            throw new BadRequestException("This item only has " + item.getQuantity() + " unit(s)");
        }
        if (assignmentRepository.findByItemIdAndUnitNumberAndReturnedAtIsNull(itemId, request.unitNumber()).isPresent()) {
            throw new BadRequestException("Unit #" + request.unitNumber() + " is already checked out");
        }

        String holderName = request.holderName() != null ? request.holderName().trim() : null;
        if (request.holderUserId() == null && (holderName == null || holderName.isEmpty())) {
            throw new BadRequestException("Pick a team member or enter the holder's name");
        }
        if (request.holderUserId() != null && userRepository.findById(request.holderUserId()).isEmpty()) {
            throw new BadRequestException("Unknown team member");
        }

        InventoryAssignment assignment = new InventoryAssignment();
        assignment.setOrganizationId(orgId);
        assignment.setItemId(itemId);
        assignment.setUnitNumber(request.unitNumber());
        assignment.setHolderUserId(request.holderUserId());
        assignment.setHolderName(holderName);
        assignment.setAssignedAt(Instant.now());
        assignment.setExpectedReturnDate(request.expectedReturnDate());
        assignment.setNotes(request.notes());
        assignmentRepository.save(assignment);
        auditService.record("inventory.unit_assign", "inventory_assignment", assignment.getId());

        List<InventoryAssignment> active = assignmentRepository.findByItemIdAndReturnedAtIsNull(itemId);
        return toResponse(item, active, resolveHolderNames(active));
    }

    @Transactional
    public InventoryItemResponse returnUnit(UUID assignmentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        InventoryAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, orgId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        if (assignment.getReturnedAt() != null) {
            throw new BadRequestException("This unit was already returned");
        }
        assignment.setReturnedAt(Instant.now());
        assignmentRepository.save(assignment);
        auditService.record("inventory.unit_return", "inventory_assignment", assignment.getId());

        InventoryItem item = itemRepository.findByIdAndOrganizationId(assignment.getItemId(), orgId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));
        List<InventoryAssignment> active = assignmentRepository.findByItemIdAndReturnedAtIsNull(item.getId());
        return toResponse(item, active, resolveHolderNames(active));
    }

    private Map<UUID, String> resolveHolderNames(List<InventoryAssignment> assignments) {
        return userRepository.findAllById(
                        assignments.stream().map(InventoryAssignment::getHolderUserId)
                                .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private static void applyRequest(InventoryItem item, InventoryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim() : null);
        item.setQuantity(request.quantity());
        item.setNotes(request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null);
    }

    private static InventoryItemResponse toResponse(InventoryItem item,
                                                    List<InventoryAssignment> activeAssignments,
                                                    Map<UUID, String> holderNames) {
        Map<Integer, InventoryAssignment> byUnit = activeAssignments.stream()
                .collect(Collectors.toMap(InventoryAssignment::getUnitNumber, Function.identity()));

        LocalDate today = LocalDate.now();
        List<InventoryUnitStatus> units = new ArrayList<>(item.getQuantity());
        int out = 0;
        int overdueCount = 0;
        for (int unit = 1; unit <= item.getQuantity(); unit++) {
            InventoryAssignment a = byUnit.get(unit);
            if (a == null) {
                units.add(InventoryUnitStatus.available(unit));
                continue;
            }
            out++;
            long daysHeld = Duration.between(a.getAssignedAt(), Instant.now()).toDays();
            boolean overdue = a.getExpectedReturnDate() != null && a.getExpectedReturnDate().isBefore(today);
            if (overdue) overdueCount++;
            String holder = a.getHolderUserId() != null
                    ? holderNames.getOrDefault(a.getHolderUserId(), a.getHolderName())
                    : a.getHolderName();
            units.add(new InventoryUnitStatus(unit, a.getId(), a.getHolderUserId(), holder,
                    a.getAssignedAt(), a.getExpectedReturnDate(), daysHeld, overdue, a.getNotes()));
        }

        return new InventoryItemResponse(item.getId(), item.getName(), item.getCategory(),
                item.getQuantity(), item.getNotes(), out, overdueCount, units);
    }
}
