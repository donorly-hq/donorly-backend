package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pledge_cards")
@Getter
@Setter
public class PledgeCard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "image_url")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_json", columnDefinition = "jsonb")
    private String extractedJson;

    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String notes;

    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = "pending";

    /** Staff member responsible for chasing this card (required by workflow). */
    @Column(name = "point_of_contact_user_id")
    private UUID pointOfContactUserId;

    @Column(name = "follow_up_count", nullable = false)
    private int followUpCount = 0;

    /** When the card entered the pending queue — drives the 24h auto-approve. */
    @Column(name = "pending_since")
    private Instant pendingSince;

    /** Import batch label, e.g. "Pilot 1200". */
    private String batch;

    /* ── automated donor reminders ─────────────────────────── */

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

    /** Set when the donor clicks "stop reminding me" — a human takes over. */
    @Column(name = "reminders_paused", nullable = false)
    private boolean remindersPaused = false;

    /** Date the donor promised to pay by (from the response page). */
    @Column(name = "promised_date")
    private LocalDate promisedDate;
}
