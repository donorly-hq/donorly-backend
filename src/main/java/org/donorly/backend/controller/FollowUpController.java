package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.FollowUpRequest;
import org.donorly.backend.dto.FollowUpUpdateRequest;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.service.FollowUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/follow-ups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping
    @PreAuthorize("hasAuthority('followups.read')")
    public List<FollowUp> list(@RequestParam(required = false) String status) {
        return status != null ? followUpService.listByStatus(status) : followUpService.list();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('followups.read')")
    public List<FollowUp> mine() {
        return followUpService.listMine();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.read')")
    public FollowUp get(@PathVariable UUID id) {
        return followUpService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('followups.write')")
    public ResponseEntity<FollowUp> create(@Valid @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(followUpService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.write')")
    public FollowUp update(@PathVariable UUID id, @RequestBody FollowUpUpdateRequest request) {
        return followUpService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.write')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        followUpService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
