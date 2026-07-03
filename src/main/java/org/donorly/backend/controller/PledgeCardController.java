package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PledgeCardRequest;
import org.donorly.backend.dto.PledgeCardResponse;
import org.donorly.backend.service.PledgeCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pledge-cards")
@RequiredArgsConstructor
public class PledgeCardController {

    private final PledgeCardService pledgeCardService;

    @GetMapping
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<PledgeCardResponse> list() {
        return pledgeCardService.list();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<PledgeCardResponse> listPending() {
        return pledgeCardService.listPending();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pledges.read')")
    public PledgeCardResponse get(@PathVariable UUID id) {
        return pledgeCardService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<PledgeCardResponse> create(@Valid @RequestBody PledgeCardRequest request) {
        return ResponseEntity.ok(pledgeCardService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('pledges.write')")
    public PledgeCardResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return pledgeCardService.updateStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        pledgeCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
