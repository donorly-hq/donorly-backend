package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.FollowUpRequest;
import org.donorly.backend.dto.FollowUpResponse;
import org.donorly.backend.dto.FollowUpUpdateRequest;
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
    public List<FollowUpResponse> list(@RequestParam(required = false) String status) {
        var followUps = status != null ? followUpService.listByStatus(status) : followUpService.list();
        return followUps.stream().map(FollowUpResponse::from).toList();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('followups.read')")
    public List<FollowUpResponse> mine() {
        return followUpService.listMine().stream().map(FollowUpResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.read')")
    public FollowUpResponse get(@PathVariable UUID id) {
        return FollowUpResponse.from(followUpService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('followups.write')")
    public ResponseEntity<FollowUpResponse> create(@Valid @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(FollowUpResponse.from(followUpService.create(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.write')")
    public FollowUpResponse update(@PathVariable UUID id, @RequestBody FollowUpUpdateRequest request) {
        return FollowUpResponse.from(followUpService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('followups.write')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        followUpService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
