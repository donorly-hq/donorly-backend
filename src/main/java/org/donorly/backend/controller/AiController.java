package org.donorly.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.AiConversation;
import org.donorly.backend.model.AiInsight;
import org.donorly.backend.service.AiInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiInsightService aiInsightService;

    // ---- Settings ----------------------------------------------------------

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('ai.use')")
    public ResponseEntity<Map<String, Object>> settings() {
        return ResponseEntity.ok(Map.of(
                "enabled", aiInsightService.isAiEnabled()
        ));
    }

    @PostMapping("/settings/enable")
    @PreAuthorize("hasAuthority('ai.admin')")
    public ResponseEntity<?> enable() {
        aiInsightService.setAiEnabled(true);
        return ResponseEntity.ok(Map.of("enabled", true));
    }

    @PostMapping("/settings/disable")
    @PreAuthorize("hasAuthority('ai.admin')")
    public ResponseEntity<?> disable() {
        aiInsightService.setAiEnabled(false);
        return ResponseEntity.ok(Map.of("enabled", false));
    }

    // ---- Insights ----------------------------------------------------------

    @GetMapping("/insights")
    @PreAuthorize("hasAuthority('ai.use')")
    public List<AiInsight> recentInsights() {
        return aiInsightService.recentInsights();
    }

    @GetMapping("/insights/org")
    @PreAuthorize("hasAuthority('ai.use')")
    public ResponseEntity<?> latestOrgInsight() {
        Optional<AiInsight> insight = aiInsightService.latestOrgInsight();
        return insight.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/insights/org")
    @PreAuthorize("hasAuthority('ai.use')")
    public AiInsight generateOrgInsight() {
        return aiInsightService.generateOrgInsight();
    }

    @GetMapping("/insights/donor/{donorId}")
    @PreAuthorize("hasAuthority('ai.use')")
    public ResponseEntity<?> latestDonorInsight(@PathVariable UUID donorId) {
        Optional<AiInsight> insight = aiInsightService.latestDonorInsight(donorId);
        return insight.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/insights/donor/{donorId}")
    @PreAuthorize("hasAuthority('ai.use')")
    public AiInsight generateDonorInsight(@PathVariable UUID donorId) {
        return aiInsightService.generateDonorInsight(donorId);
    }

    @GetMapping("/insights/campaign/{campaignId}")
    @PreAuthorize("hasAuthority('ai.use')")
    public ResponseEntity<?> latestCampaignInsight(@PathVariable UUID campaignId) {
        Optional<AiInsight> insight = aiInsightService.latestCampaignInsight(campaignId);
        return insight.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/insights/campaign/{campaignId}")
    @PreAuthorize("hasAuthority('ai.use')")
    public AiInsight generateCampaignInsight(@PathVariable UUID campaignId) {
        return aiInsightService.generateCampaignInsight(campaignId);
    }

    // ---- Chat assistant ----------------------------------------------------

    @GetMapping("/conversations")
    @PreAuthorize("hasAuthority('ai.use')")
    public List<AiConversation> conversations() {
        return aiInsightService.recentConversations();
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAuthority('ai.use')")
    public AiConversation ask(@Valid @RequestBody AskRequest request) {
        return aiInsightService.ask(request.question());
    }

    record AskRequest(@NotBlank String question) {
    }
}
