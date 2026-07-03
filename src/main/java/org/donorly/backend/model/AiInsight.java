package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ai_insights")
@Getter
@Setter
public class AiInsight extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, columnDefinition = "text")
    private String insight;

    private String model;

    /** The user who requested the insight generation (business field). */
    @Column(name = "generated_by")
    private UUID generatedBy;
}
