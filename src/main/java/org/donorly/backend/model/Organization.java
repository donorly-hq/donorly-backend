package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String vertical = "nonprofit";

    @Column(nullable = false)
    private String status = "trial";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_data", columnDefinition = "TEXT")
    private String logoData;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(nullable = false)
    private String timezone = "America/Chicago";

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
