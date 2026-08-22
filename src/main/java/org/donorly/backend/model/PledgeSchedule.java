package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One installment of a pledge, e.g. "5,000 total = 500/month for 10 months".
 * Created when a donor asks to pay in installments (manually or via an
 * interactive messaging reply).
 */
@Entity
@Table(name = "pledge_schedules")
@Getter
@Setter
public class PledgeSchedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "pledge_id", nullable = false)
    private UUID pledgeId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount_due", nullable = false)
    private BigDecimal amountDue;

    /** pending | paid | overdue | cancelled */
    @Column(nullable = false)
    private String status = "pending";
}
