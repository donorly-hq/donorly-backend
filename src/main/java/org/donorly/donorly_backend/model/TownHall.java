package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "townhalls")
public class TownHall {
    @Id
    private String id;

    private String responsiblePersonName;
    private String responsiblePersonPhone;

    private String venue;
    private String address;     // human-readable, from Google Places Autocomplete
    private String placeId;     // Google Places place_id
    private Double lat;
    private Double lng;

    private LocalDate eventDate;
    private String eventTime;   // e.g. "18:30"
    private Integer durationMinutes;
    private Integer rsvpCount;

    private String hostAmbassadorId;
    private String status; // planned / confirmed / completed / cancelled

    private LocalDateTime createdAt;
}
