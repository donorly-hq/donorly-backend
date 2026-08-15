package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A snoozed dashboard suggestion: hidden for this org until {@code dismissedUntil}. */
@Entity
@Table(name = "dismissed_suggestions")
@Getter
@Setter
public class DismissedSuggestion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "suggestion_key", nullable = false, length = 120)
    private String suggestionKey;

    @Column(name = "dismissed_by")
    private UUID dismissedBy;

    @Column(name = "dismissed_until", nullable = false)
    private Instant dismissedUntil;
}
