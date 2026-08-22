package org.donorly.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record CommunicationMessageResponse(
        UUID id,
        String channel,
        String recipient,
        UUID donorId,
        String donorName,
        UUID templateId,
        String templateName,
        String subject,
        String body,
        String status,
        String errorMessage,
        String direction,       // outbound | inbound
        UUID campaignId,
        Instant sentAt,
        Instant createdAt
) {
}
