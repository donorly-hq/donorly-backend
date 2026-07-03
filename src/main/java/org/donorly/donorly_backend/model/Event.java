package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "townhalls")
public class TownHall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String responsiblePersonName;

    private String responsiblePersonPhone;

    private String venue;

    private String address;

    private String placeId;

    private Double lat;

    private Double lng;

    private LocalDate eventDate;

    private LocalTime eventTime;

    private Integer durationMinutes;

    private Integer rsvpCount;

    private String hostAmbassadorId;

    private String status = "Scheduled";
}
