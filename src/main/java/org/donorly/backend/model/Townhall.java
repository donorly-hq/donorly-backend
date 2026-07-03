package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "townhalls")
@Getter
@Setter
public class Townhall extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "person_name", nullable = false)
    private String personName;

    private String phone;

    private String venue;

    private String address;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "host_ambassador_user_id")
    private UUID hostAmbassadorUserId;

    @Column(name = "expected_rsvps")
    private Integer expectedRsvps;

    @Column(columnDefinition = "text")
    private String notes;
}
