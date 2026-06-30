package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.Ambassador;
import org.donorly.donorly_backend.security.JwtAuthFilter.AuthenticatedUser;
import org.donorly.donorly_backend.service.AmbassadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ambassadors")
@CrossOrigin(origins = "*")
public class AmbassadorController {

    @Autowired
    private AmbassadorService ambassadorService;

    @GetMapping
    public List<Ambassador> getAll() {
        return ambassadorService.getAllAmbassadors();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ambassador> getById(@PathVariable String id) {
        return ambassadorService.getAmbassadorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Ambassador create(@RequestBody Ambassador ambassador) {
        return ambassadorService.createAmbassador(ambassador);
    }

    @PutMapping("/{id}")
    public Ambassador update(@PathVariable String id, @RequestBody Ambassador ambassador) {
        return ambassadorService.updateAmbassador(id, ambassador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ambassadorService.deleteAmbassador(id);
        return ResponseEntity.ok().build();
    }

    // --- Ambassador hierarchy ---

    private AuthenticatedUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (AuthenticatedUser) auth.getPrincipal() : null;
    }

    @PostMapping("/{id}/sub-ambassadors")
    public ResponseEntity<?> createSubAmbassador(@PathVariable String id, @RequestBody Ambassador newAmbassador) {
        AuthenticatedUser user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));

        boolean isAdmin = "ADMIN".equals(user.role());
        boolean isSelf = id.equals(user.ambassadorId());
        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the ambassador themself (or Admin) can create a sub-ambassador here"));
        }

        try {
            return ResponseEntity.ok(ambassadorService.createSubAmbassador(id, newAmbassador));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Root-level ambassador creation — Admin only (ambassadors can create
    // sub-ambassadors via the endpoint above, but not new root-level peers).
    @PostMapping("/root")
    public ResponseEntity<?> createRootAmbassador(@RequestBody Ambassador newAmbassador) {
        AuthenticatedUser user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        if (!"ADMIN".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only Admin can create a root-level ambassador"));
        }
        return ResponseEntity.ok(ambassadorService.createRootAmbassador(newAmbassador));
    }

    @GetMapping("/{id}/downline")
    public ResponseEntity<?> getDownline(@PathVariable String id) {
        AuthenticatedUser user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));

        boolean isAdmin = "ADMIN".equals(user.role());
        if (!isAdmin && !ambassadorService.isSelfOrAncestor(user.ambassadorId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authorized"));
        }
        return ResponseEntity.ok(ambassadorService.getDownline(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable String id) {
        AuthenticatedUser user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));

        boolean isAdmin = "ADMIN".equals(user.role());
        try {
            return ResponseEntity.ok(ambassadorService.deactivate(id, user.ambassadorId(), isAdmin));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // Full handover: the ambassador hands their entire position (donors,
    // pledges, downline) over to another ambassador, and is locked out
    // afterward. Only the ambassador themself may trigger this for their
    // own account — not Admin, not anyone else.
    @PostMapping("/{id}/handover-to/{targetId}")
    public ResponseEntity<?> handoverTo(@PathVariable String id, @PathVariable String targetId) {
        AuthenticatedUser user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));

        boolean isSelf = id.equals(user.ambassadorId());
        if (!isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the ambassador themself can hand over their own account"));
        }

        try {
            return ResponseEntity.ok(ambassadorService.handoverTo(id, targetId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
