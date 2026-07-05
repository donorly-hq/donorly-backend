package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.service.CsvExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final CsvExportService csvExportService;

    @GetMapping("/donors")
    @PreAuthorize("hasAuthority('donors.export')")
    public ResponseEntity<byte[]> donors() {
        return csv("donors", csvExportService.donorsCsv());
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('payments.manage')")
    public ResponseEntity<byte[]> payments() {
        return csv("payments", csvExportService.paymentsCsv());
    }

    @GetMapping("/pledges")
    @PreAuthorize("hasAuthority('pledges.read')")
    public ResponseEntity<byte[]> pledges() {
        return csv("pledges", csvExportService.pledgesCsv());
    }

    private static ResponseEntity<byte[]> csv(String name, String content) {
        String filename = name + "-" + LocalDate.now() + ".csv";
        // BOM so Excel opens UTF-8 correctly
        byte[] body = ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(body);
    }
}
