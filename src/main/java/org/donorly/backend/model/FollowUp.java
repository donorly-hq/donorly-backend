package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follow_ups")
@Getter
@Setter
public class FollowUp extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(nullable = false)
    private String status = "open";

    private String outcome;

    private String notes;
}
