package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.dto.AmbassadorCreateRequest;
import org.donorly.donorly_backend.dto.AmbassadorResponseDto;
import org.donorly.donorly_backend.service.AmbassadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ambassadors")
@RequiredArgsConstructor
public class AmbassadorController {

    private final AmbassadorService ambassadorService;

    @GetMapping
    public List<AmbassadorResponseDto> getAll() {
        return ambassadorService.getAllDto();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmbassadorResponseDto> getById(@PathVariable UUID id) {
        return ambassadorService.getByIdDto(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public AmbassadorResponseDto create(@RequestBody AmbassadorCreateRequest request) {
        return ambassadorService.createRootFromRequest(request);
    }

    @PostMapping("/root")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbassadorResponseDto createRoot(@RequestBody AmbassadorCreateRequest request) {
        return ambassadorService.createRootFromRequest(request);
    }

    @PostMapping("/{id}/sub-ambassadors")
    public AmbassadorResponseDto createSub(@PathVariable UUID id, @RequestBody AmbassadorCreateRequest request) {
        return ambassadorService.createSubFromRequest(id, request);
    }

    @GetMapping("/{id}/downline")
    public List<AmbassadorResponseDto> getDownline(@PathVariable UUID id) {
        return ambassadorService.getDownlineDto(id);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AmbassadorResponseDto> deactivate(@PathVariable UUID id) {
        ambassadorService.deactivate(id);
        return ambassadorService.getByIdDto(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/handover-to/{targetId}")
    public ResponseEntity<?> handover(@PathVariable UUID id, @PathVariable UUID targetId) {
        ambassadorService.handoverTo(id, targetId);
        return ResponseEntity.ok("Handover complete");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ambassadorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
