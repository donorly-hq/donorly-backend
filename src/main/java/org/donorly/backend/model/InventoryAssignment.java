package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A physical unit of an inventory item checked out to someone. Active while returned_at is null. */
@Entity
@Table(name = "inventory_assignments")
@Getter
@Setter
public class InventoryAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "unit_number", nullable = false)
    private int unitNumber;

    /** Team member holding the unit; null when the holder is recorded by name only. */
    @Column(name = "holder_user_id")
    private UUID holderUserId;

    @Column(name = "holder_name")
    private String holderName;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    private String notes;
}
