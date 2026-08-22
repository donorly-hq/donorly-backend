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

    private String state;

    @Column(columnDefinition = "text")
    private String address;

    /** Point of contact — the staff member responsible for this donor. */
    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "is_major_donor", nullable = false)
    private boolean majorDonor = false;

    /** confirmed | potential | re_registering */
    @Column(nullable = false)
    private String bucket = "confirmed";

    /** ok | non_compliant | claims_paid | non_responsive */
    @Column(name = "compliance_status", nullable = false)
    private String complianceStatus = "ok";

    @Column(name = "donor_type", nullable = false)
    private String donorType = "individual";

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "lifetime_giving", nullable = false)
    private BigDecimal lifetimeGiving = BigDecimal.ZERO;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
