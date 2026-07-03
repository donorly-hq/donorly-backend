package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_registrations")
@Getter
@Setter
public class EventRegistration extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "donor_id")
    private UUID donorId;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "party_size", nullable = false)
    private Integer partySize = 1;

    @Column(nullable = false)
    private String status = "registered";

    @Column(name = "check_in_code", nullable = false)
    private String checkInCode;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by")
    private UUID checkedInBy;

    private String notes;
}
