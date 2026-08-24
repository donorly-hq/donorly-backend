package org.donorly.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time link a donor clicks from a reminder email to respond about a
 * pledge card ("I already paid" / "I'll pay by ..." / "stop reminders").
 * Kept separate from {@link AuthToken}: donors are not portal users.
 */
@Entity
@Table(name = "donor_action_tokens")
@Getter
@Setter
public class DonorActionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** Exactly one of pledgeCardId / pledgeId is set (DB check constraint). */
    @Column(name = "pledge_card_id")
    private UUID pledgeCardId;

    @Column(name = "pledge_id")
    private UUID pledgeId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
