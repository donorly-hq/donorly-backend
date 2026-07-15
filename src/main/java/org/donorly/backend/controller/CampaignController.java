package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.CampaignDashboardResponse;
import org.donorly.backend.dto.CampaignRequest;
import org.donorly.backend.dto.CampaignResponse;
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
}
