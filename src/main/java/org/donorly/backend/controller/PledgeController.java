package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PledgeRequest;
import org.donorly.backend.dto.PledgeUpdateRequest;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.service.PledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PledgeController {

    private final PledgeService pledgeService;
    private final org.donorly.backend.service.PledgeReminderService reminderService;

    /** Without {@code page}, returns the full list (legacy behavior); with it, a page envelope. */
    @GetMapping("/pledges")
    @PreAuthorize("hasAuthority('pledges.read')")
    public Object list(@RequestParam(required = false) Integer page,
                       @RequestParam(defaultValue = "50") int size) {
        if (page == null) {
            return pledgeService.list();
        }
        return pledgeService.page(page, size);
    }

    @GetMapping("/pledges/{id}")
    @PreAuthorize("hasAuthority('pledges.read')")
    public Pledge get(@PathVariable UUID id) {
        return pledgeService.get(id);
    }

    @GetMapping("/campaigns/{campaignId}/pledges")
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<Pledge> listByCampaign(@PathVariable UUID campaignId) {
        return pledgeService.listByCampaign(campaignId);
    }

    @GetMapping("/donors/{donorId}/pledges")
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<Pledge> listByDonor(@PathVariable UUID donorId) {
        return pledgeService.listByDonor(donorId);
    }

    @PostMapping("/campaigns/{campaignId}/pledges")
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<Pledge> create(@PathVariable UUID campaignId,
                                         @Valid @RequestBody PledgeRequest request) {
        return ResponseEntity.ok(pledgeService.create(campaignId, request));
    }

    /** One-tap entry for live events: find-or-create donor + pledge in one call. */
    @PostMapping("/pledges/quick")
    @PreAuthorize("hasAuthority('pledges.write')")
    public org.donorly.backend.dto.QuickPledgeResponse quick(
            @Valid @RequestBody org.donorly.backend.dto.QuickPledgeRequest request) {
        return pledgeService.quickCreate(request);
    }

    /** Projector-friendly live tally for a campaign. */
    @GetMapping("/campaigns/{campaignId}/live")
    @PreAuthorize("hasAuthority('pledges.read')")
    public org.donorly.backend.dto.CampaignLiveResponse live(@PathVariable UUID campaignId) {
        return pledgeService.liveProgress(campaignId);
    }

    /** Manual pledge reminder email, triggered from the UI. */
    @PostMapping("/pledges/{id}/remind")
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<?> remind(@PathVariable UUID id) {
        reminderService.sendReminder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/pledges/{id}")
    @PreAuthorize("hasAuthority('pledges.write')")
    public Pledge update(@PathVariable UUID id, @RequestBody PledgeUpdateRequest request) {
        return pledgeService.update(id, request);
    }

    @DeleteMapping("/pledges/{id}")
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        pledgeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
