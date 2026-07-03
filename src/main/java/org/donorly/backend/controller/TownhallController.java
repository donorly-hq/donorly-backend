package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.TownhallRequest;
import org.donorly.backend.model.Townhall;
import org.donorly.backend.service.TownhallService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/townhalls")
@RequiredArgsConstructor
public class TownhallController {

    private final TownhallService townhallService;

    @GetMapping
    @PreAuthorize("hasAuthority('townhalls.read')")
    public List<Townhall> list() {
        return townhallService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('townhalls.read')")
    public Townhall get(@PathVariable UUID id) {
        return townhallService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('townhalls.manage')")
    public ResponseEntity<Townhall> create(@Valid @RequestBody TownhallRequest request) {
        return ResponseEntity.ok(townhallService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('townhalls.manage')")
    public Townhall update(@PathVariable UUID id, @Valid @RequestBody TownhallRequest request) {
        return townhallService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('townhalls.manage')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        townhallService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
