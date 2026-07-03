package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receipts")
@Getter
@Setter
public class Receipt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "issued_to", nullable = false)
    private String issuedTo;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    /** Staff member who issued the receipt (business field). */
    @Column(name = "issued_by")
    private UUID issuedBy;
}
