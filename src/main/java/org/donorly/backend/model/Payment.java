package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "pledge_id", nullable = false)
    private UUID pledgeId;

    @Column(name = "donor_id", nullable = false)
    private UUID donorId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate = LocalDate.now();

    private String reference;

    @Column(columnDefinition = "text")
    private String notes;

    /** Staff member who physically recorded the payment (business field). */
    @Column(name = "recorded_by")
    private UUID recordedBy;
}
