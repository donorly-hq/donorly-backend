package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.OrgMemberSummary;
import org.donorly.backend.dto.OrganizationRequest;
import org.donorly.backend.dto.OrganizationResponse;
import org.donorly.backend.dto.SetOwnerRequest;
import org.donorly.backend.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform-level organization management.
 * All endpoints require the {@code platform.organizations.manage} permission
 * which is only granted to the {@code platform_super_admin} role.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('platform.organizations.manage')")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public List<OrganizationResponse> list() {
        return organizationService.listAll();
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable UUID id) {
        return organizationService.getById(id);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.create(request));
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody OrganizationRequest request) {
        return organizationService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrganizationResponse updateStatus(@PathVariable UUID id,
                                             @RequestBody Map<String, String> body) {
        return organizationService.updateStatus(id, body.get("status"));
    }

    @PutMapping("/{id}/owner")
    public OrganizationResponse setOwner(@PathVariable UUID id,
                                         @Valid @RequestBody SetOwnerRequest request) {
        return organizationService.setOwner(id, request);
    }

    @PutMapping("/{id}/owner/{userId}")
    public OrganizationResponse promoteOwner(@PathVariable UUID id, @PathVariable UUID userId) {
        return organizationService.promoteOwner(id, userId);
    }

    @GetMapping("/{id}/members")
    public List<OrgMemberSummary> listMembers(@PathVariable UUID id) {
        return organizationService.listMembers(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
