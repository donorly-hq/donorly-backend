package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pledges")
@Getter
@Setter
public class Pledge extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "collected_amount", nullable = false)
    private BigDecimal collectedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String frequency = "one_time";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private String status = "pending";

    private String source;

    private String notes;
}
