package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "campaign_type", nullable = false)
    private String campaignType = "general";

    @Column(name = "goal_amount", nullable = false)
    private BigDecimal goalAmount = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "managed_by_user_id")
    private UUID managedByUserId;

    @Column(nullable = false)
    private String status = "draft";
}
