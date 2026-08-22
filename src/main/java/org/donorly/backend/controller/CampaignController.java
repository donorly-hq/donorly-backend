package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.CampaignAudienceResponse;
import org.donorly.backend.dto.CampaignDashboardResponse;
import org.donorly.backend.dto.CampaignMessagingRequest;
import org.donorly.backend.dto.CampaignMessagingResponse;
import org.donorly.backend.dto.CampaignRequest;
import org.donorly.backend.dto.CampaignResponse;
import org.donorly.backend.dto.CampaignTargetRequest;
import org.donorly.backend.service.CampaignAudienceService;
import org.donorly.backend.service.CampaignService;
import org.donorly.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final DashboardService dashboardService;
    private final CampaignAudienceService audienceService;

    @GetMapping
    @PreAuthorize("hasAuthority('campaigns.read')")
    public List<CampaignResponse> list() {
        return campaignService.list().stream().map(CampaignResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('campaigns.read')")
    public CampaignResponse get(@PathVariable UUID id) {
        return CampaignResponse.from(campaignService.get(id));
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAuthority('reports.view')")
    public CampaignDashboardResponse dashboard(@PathVariable UUID id) {
        return dashboardService.campaignDashboard(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignResponse update(@PathVariable UUID id, @Valid @RequestBody CampaignRequest request) {
        return CampaignResponse.from(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== audience targeting =====

    @GetMapping("/{id}/audience")
    @PreAuthorize("hasAuthority('campaigns.read')")
    public CampaignAudienceResponse audience(@PathVariable UUID id) {
        return audienceService.audience(id);
    }

    @PostMapping("/{id}/audience")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignAudienceResponse addTarget(@PathVariable UUID id,
                                              @RequestBody CampaignTargetRequest request) {
        return audienceService.addTarget(id, request);
    }

    @DeleteMapping("/{id}/audience/{targetId}")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignAudienceResponse removeTarget(@PathVariable UUID id, @PathVariable UUID targetId) {
        return audienceService.removeTarget(id, targetId);
    }

    // ===== messaging configuration =====

    @GetMapping("/{id}/messaging")
    @PreAuthorize("hasAuthority('campaigns.read')")
    public CampaignMessagingResponse messaging(@PathVariable UUID id) {
        return CampaignMessagingResponse.from(audienceService.messaging(id));
    }

    @PutMapping("/{id}/messaging")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignMessagingResponse updateMessaging(@PathVariable UUID id,
                                                     @RequestBody CampaignMessagingRequest request) {
        return CampaignMessagingResponse.from(audienceService.updateMessaging(id, request));
    }
}
