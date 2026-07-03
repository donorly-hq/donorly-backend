package org.donorly.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AmbassadorDashboardResponse(
        /* ── work counters ─────────────────────────────────── */
        long assignedDonors,
        long openFollowUps,
        long totalFollowUps,
        long completedFollowUps,
        long pledgeCount,
        BigDecimal totalPledged,
        BigDecimal totalCollected,
        BigDecimal outstanding,

        /* ── upcoming snapshots ────────────────────────────── */
        List<EventItem>    upcomingEvents,
        List<TownhallItem> upcomingTownhalls,
        List<CampaignItem> activeCampaigns
) {

    public record EventItem(
            UUID id, String name, String location, String eventType,
            String status, Instant startsAt, Instant endsAt
    ) {}

    public record TownhallItem(
            UUID id, String personName, String address,
            LocalDate eventDate, LocalTime eventTime, Integer durationMinutes
    ) {}

    public record CampaignItem(
            UUID id, String name, String campaignType, String status,
            BigDecimal goalAmount, LocalDate startDate, LocalDate endDate
    ) {}
}
