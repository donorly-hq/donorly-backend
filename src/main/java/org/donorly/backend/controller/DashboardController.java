package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.AmbassadorDashboardResponse;
import org.donorly.backend.dto.CampaignManagerDashboardResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.donorly.backend.dto.OrgDashboardResponse;
import org.donorly.backend.dto.SetupProgressResponse;
import org.donorly.backend.dto.SuggestionResponse;
import org.donorly.backend.service.DashboardService;
import org.donorly.backend.service.SetupProgressService;
import org.donorly.backend.service.SuggestionService;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SetupProgressService setupProgressService;
    private final SuggestionService suggestionService;

    @GetMapping
    @PreAuthorize("hasAuthority('reports.view')")
    public OrgDashboardResponse dashboard() {
        return dashboardService.orgDashboard();
    }

    @GetMapping("/setup")
    @PreAuthorize("hasAuthority('reports.view')")
    public SetupProgressResponse setupProgress() {
        return setupProgressService.progressFor(TenantContext.requireOrganizationId());
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAuthority('reports.view')")
    public List<SuggestionResponse> suggestions() {
        return suggestionService.currentSuggestions();
    }

    @PostMapping("/suggestions/dismiss")
    @PreAuthorize("hasAuthority('reports.view')")
    public ResponseEntity<?> dismissSuggestion(@Valid @RequestBody DismissRequest request) {
        suggestionService.dismiss(request.key());
        return ResponseEntity.noContent().build();
    }

    record DismissRequest(@NotBlank String key) {
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('followups.read')")
    public AmbassadorDashboardResponse myDashboard() {
        return dashboardService.ambassadorDashboard();
    }

    @GetMapping("/campaign-manager")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignManagerDashboardResponse campaignManagerDashboard() {
        return dashboardService.campaignManagerDashboard();
    }
}
