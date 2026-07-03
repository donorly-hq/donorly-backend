package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "donor_notes")
@Getter
@Setter
public class DonorNote extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "note_text", nullable = false)
    private String noteText;

    @Column(name = "note_type")
    private String noteType;

    @Column(nullable = false)
    private String visibility = "team";
}
