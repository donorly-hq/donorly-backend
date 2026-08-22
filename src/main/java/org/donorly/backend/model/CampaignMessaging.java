package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-campaign automated-messaging configuration (1:1 with the campaign).
 * Channels are stored as a comma-separated list (e.g. "whatsapp,email") to
 * keep the schema simple; {@code getChannelList()} exposes them parsed.
 */
@Entity
@Table(name = "campaign_messaging")
@Getter
@Setter
public class CampaignMessaging extends AuditableEntity {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "message_content", columnDefinition = "text")
    private String messageContent;

    @Column(name = "flyer_url", columnDefinition = "text")
    private String flyerUrl;

    @Column(name = "payment_link", columnDefinition = "text")
    private String paymentLink;

    /** daily | every_2_days | weekly */
    @Column(nullable = false)
    private String frequency = "weekly";

    /** Comma-separated subset of: whatsapp, sms, robocall, email. */
    @Column(nullable = false)
    private String channels = "email";

    @Column(nullable = false)
    private boolean personalized = true;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Transient
    public java.util.List<String> getChannelList() {
        if (channels == null || channels.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
