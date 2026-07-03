package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donors")
@Getter
@Setter
public class Donor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String email;

    private String phone;

    private String city;

    @Column(name = "donor_type", nullable = false)
    private String donorType = "individual";

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "lifetime_giving", nullable = false)
    private BigDecimal lifetimeGiving = BigDecimal.ZERO;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
