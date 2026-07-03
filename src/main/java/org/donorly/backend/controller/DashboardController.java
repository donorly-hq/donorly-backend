package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.AmbassadorDashboardResponse;
import org.donorly.backend.dto.CampaignManagerDashboardResponse;
import org.donorly.backend.dto.OrgDashboardResponse;
import org.donorly.backend.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public OrgDashboardResponse dashboard() {
        return dashboardService.orgDashboard();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public AmbassadorDashboardResponse myDashboard() {
        return dashboardService.ambassadorDashboard();
    }

    @GetMapping("/campaign-manager")
    @PreAuthorize("hasAuthority('campaigns.manage')")
    public CampaignManagerDashboardResponse campaignManagerDashboard() {
        return dashboardService.campaignManagerDashboard();
    }
}
