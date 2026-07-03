package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communication_messages")
@Getter
@Setter
public class CommunicationMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "template_id")
    private UUID templateId;

    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private String status = "queued";

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** The user who triggered the send action (business field, distinct from createdBy). */
    @Column(name = "sent_by")
    private UUID sentBy;

    @Column(name = "sent_at")
    private Instant sentAt;
}
