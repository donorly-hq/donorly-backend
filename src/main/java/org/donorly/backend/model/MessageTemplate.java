package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "message_templates")
@Getter
@Setter
public class MessageTemplate extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String channel = "email";

    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}
