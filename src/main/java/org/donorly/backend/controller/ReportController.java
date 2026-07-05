package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.FundraisingReportResponse;
import org.donorly.backend.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/fundraising")
    @PreAuthorize("hasAuthority('reports.view')")
    public FundraisingReportResponse fundraising() {
        return reportService.fundraisingReport();
    }

    @GetMapping("/year-end")
    @PreAuthorize("hasAuthority('reports.view')")
    public org.donorly.backend.dto.YearEndStatementResponse yearEnd(
            @org.springframework.web.bind.annotation.RequestParam int year) {
        return reportService.yearEndStatements(year);
    }
}
