package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PledgeCardRequest;
import org.donorly.backend.dto.PledgeCardResponse;
import org.donorly.backend.dto.PledgeCardScanRequest;
import org.donorly.backend.dto.PledgeCardScanResponse;
import org.donorly.backend.service.PledgeCardScanService;
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
    private final PledgeCardScanService pledgeCardScanService;
    private final org.donorly.backend.service.PledgeCardImportService pledgeCardImportService;

    @GetMapping
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<PledgeCardResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(required = false) java.time.Instant enteredAfter,
            @RequestParam(required = false) java.time.Instant enteredBefore,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String compliance) {
        return pledgeCardService.list(new org.donorly.backend.dto.PledgeCardFilter(
                status, batch, minAmount, maxAmount, enteredAfter, enteredBefore, location, compliance));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<PledgeCardResponse> listPending() {
        return pledgeCardService.listPending();
    }

    /** Manual-verification queue: needs_verification / claims_paid / non_responsive. */
    @GetMapping("/needs-verification")
    @PreAuthorize("hasAuthority('pledges.read')")
    public List<PledgeCardResponse> listNeedsVerification() {
        return pledgeCardService.listNeedsVerification();
    }

    @GetMapping("/auto-approve-policy")
    @PreAuthorize("hasAuthority('pledges.read')")
    public Map<String, Object> autoApprovePolicy() {
        return pledgeCardService.autoApprovePolicy();
    }

    @PutMapping("/auto-approve-policy")
    @PreAuthorize("hasAuthority('pledges.write')")
    public Map<String, Object> updateAutoApprovePolicy(@RequestBody Map<String, Object> body) {
        boolean autoApprove = Boolean.TRUE.equals(body.get("autoApprove"));
        Integer hours = body.get("hours") instanceof Number n ? n.intValue() : null;
        return pledgeCardService.updateAutoApprovePolicy(autoApprove, hours);
    }

    @GetMapping("/reminder-policy")
    @PreAuthorize("hasAuthority('pledges.read')")
    public Map<String, Object> reminderPolicy() {
        return pledgeCardService.reminderPolicy();
    }

    @PutMapping("/reminder-policy")
    @PreAuthorize("hasAuthority('pledges.write')")
    public Map<String, Object> updateReminderPolicy(@RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        Integer intervalDays = body.get("intervalDays") instanceof Number n ? n.intValue() : null;
        Integer maxAttempts = body.get("maxAttempts") instanceof Number n ? n.intValue() : null;
        return pledgeCardService.updateReminderPolicy(enabled, intervalDays, maxAttempts);
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

    /** Bulk import from a spreadsheet: rows become cards in the normal pending queue. */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('pledges.write')")
    public org.donorly.backend.dto.PledgeCardImportResult importCards(
            @Valid @RequestBody org.donorly.backend.dto.PledgeCardImportRequest request) {
        return pledgeCardImportService.importCards(request);
    }

    /** Photo of a paper pledge card in, AI-suggested field values out. Nothing is saved yet. */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('pledges.write')")
    public PledgeCardScanResponse scan(@Valid @RequestBody PledgeCardScanRequest request) {
        return pledgeCardScanService.scan(request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('pledges.write')")
    public PledgeCardResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return pledgeCardService.updateStatus(id, body.get("status"));
    }

    /** Bumps the follow-up counter after a chase attempt. */
    @PostMapping("/{id}/follow-up")
    @PreAuthorize("hasAuthority('pledges.write')")
    public PledgeCardResponse recordFollowUp(@PathVariable UUID id) {
        return pledgeCardService.recordFollowUp(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pledges.write')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        pledgeCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
