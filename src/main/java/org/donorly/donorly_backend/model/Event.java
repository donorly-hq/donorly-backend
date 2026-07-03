package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    private LocalDate eventDate;

    private LocalTime eventTime;

    private String location;

    private String ambassadorId;

    private String status = "Upcoming";
}
