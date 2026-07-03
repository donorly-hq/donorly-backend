package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "donor_assignments")
@Getter
@Setter
public class DonorAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(name = "ambassador_user_id", nullable = false)
    private UUID ambassadorUserId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(nullable = false)
    private String status = "active";
}
