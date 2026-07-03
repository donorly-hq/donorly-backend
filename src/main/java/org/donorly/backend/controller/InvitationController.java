package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.AcceptInviteRequest;
import org.donorly.backend.dto.InvitationInfoResponse;
import org.donorly.backend.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public (unauthenticated) endpoints for accepting a team invitation.
 * Registered as permit-all in {@link org.donorly.backend.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final TeamService teamService;

    @GetMapping("/{token}")
    public InvitationInfoResponse info(@PathVariable String token) {
        return teamService.getInvitationInfo(token);
    }

    @PostMapping("/accept")
    public ResponseEntity<?> accept(@Valid @RequestBody AcceptInviteRequest request) {
        teamService.acceptInvitation(request);
        return ResponseEntity.ok(Map.of("message", "Invitation accepted. You can now sign in."));
    }
}
