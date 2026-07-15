package org.donorly.backend.dto;

import org.donorly.backend.model.Event;

import java.time.Instant;
import java.util.UUID;

/** API shape for an event — decouples the wire format from the JPA entity. */
public record EventResponse(
        UUID id,
        UUID organizationId,
        UUID campaignId,
        String name,
        String description,
        String eventType,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer capacity,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getOrganizationId(), e.getCampaignId(), e.getName(),
                e.getDescription(), e.getEventType(), e.getLocation(), e.getStartsAt(), e.getEndsAt(),
                e.getCapacity(), e.getStatus(), e.getCreatedAt(), e.getModifiedAt());
    }
}
