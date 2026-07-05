package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.InventoryAssignRequest;
import org.donorly.backend.dto.InventoryItemRequest;
import org.donorly.backend.dto.InventoryItemResponse;
import org.donorly.backend.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public List<InventoryItemResponse> list() {
        return inventoryService.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.write')")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.ok(inventoryService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.write')")
    public InventoryItemResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody InventoryItemRequest request) {
        return inventoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.assign')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Check a unit out to a holder — restricted to owner/admin/campaign manager. */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('inventory.assign')")
    public InventoryItemResponse assign(@PathVariable UUID id,
                                        @Valid @RequestBody InventoryAssignRequest request) {
        return inventoryService.assign(id, request);
    }

    @PostMapping("/assignments/{assignmentId}/return")
    @PreAuthorize("hasAuthority('inventory.assign')")
    public InventoryItemResponse returnUnit(@PathVariable UUID assignmentId) {
        return inventoryService.returnUnit(assignmentId);
    }
}
