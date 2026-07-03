package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.service.AmbassadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambassadors")
@RequiredArgsConstructor
public class AmbassadorController {

    private final AmbassadorService ambassadorService;

    @GetMapping
    public List<Ambassador> getAll() {
        return ambassadorService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ambassador> getById(@PathVariable String id) {
        return ambassadorService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Ambassador create(@RequestBody Ambassador ambassador) {
        return ambassadorService.save(ambassador);
    }

    @PostMapping("/root")
    @PreAuthorize("hasRole('ADMIN')")
    public Ambassador createRoot(@RequestBody Ambassador ambassador) {
        return ambassadorService.createRootAmbassador(ambassador);
    }

    @PostMapping("/{id}/sub-ambassadors")
    public Ambassador createSub(@PathVariable String id, @RequestBody Ambassador ambassador) {
        return ambassadorService.createSubAmbassador(id, ambassador);
    }

    @GetMapping("/{id}/downline")
    public List<Ambassador> getDownline(@PathVariable String id) {
        return ambassadorService.getDownline(id);
    }

    @PutMapping("/{id}")
    public Ambassador update(@PathVariable String id, @RequestBody Ambassador ambassador) {
        ambassador.setId(id);
        return ambassadorService.save(ambassador);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Ambassador> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ambassadorService.deactivate(id));
    }

    @PostMapping("/{id}/handover-to/{targetId}")
    public ResponseEntity<?> handover(@PathVariable String id, @PathVariable String targetId) {
        ambassadorService.handoverTo(id, targetId);
        return ResponseEntity.ok("Handover complete");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ambassadorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
