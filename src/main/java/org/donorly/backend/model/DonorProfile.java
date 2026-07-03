package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "donor_profiles")
@Getter
@Setter
public class DonorProfile extends AuditableEntity {

    @Id
    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    private String occupation;

    private String employer;

    @Column(name = "preferred_language")
    private String preferredLanguage;

    @Column(name = "preferred_channel")
    private String preferredChannel;

    @Column(name = "notes_private", columnDefinition = "text")
    private String notesPrivate;
}
