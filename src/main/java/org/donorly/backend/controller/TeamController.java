package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.*;
import org.donorly.backend.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/members")
    @PreAuthorize("hasAuthority('users.manage')")
    public List<TeamMemberResponse> members() {
        return teamService.listMembers();
    }

    @PatchMapping("/members/{userId}")
    @PreAuthorize("hasAuthority('users.manage')")
    public TeamMemberResponse updateMember(@PathVariable UUID userId,
                                           @RequestBody MemberUpdateRequest request) {
        return teamService.updateMember(userId, request);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('users.manage')")
    public List<RoleResponse> roles() {
        return teamService.listAssignableRoles();
    }

    /** Active members, available to donors.write holders for assignment pickers. */
    @GetMapping("/assignable")
    @PreAuthorize("hasAuthority('donors.write')")
    public List<TeamMemberResponse> assignable() {
        return teamService.listActiveMembers();
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAuthority('users.manage')")
    public List<InvitationResponse> invitations() {
        return teamService.listPendingInvitations();
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyAuthority('users.manage', 'team.invite')")
    public ResponseEntity<InvitationResponse> invite(@Valid @RequestBody InvitationRequest request) {
        return ResponseEntity.ok(teamService.invite(request));
    }

    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasAuthority('users.manage')")
    public ResponseEntity<?> revoke(@PathVariable UUID id) {
        teamService.revokeInvitation(id);
        return ResponseEntity.noContent().build();
    }
}
