package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "events")
public class Event {
    @Id
    private String id;
    private String title;
    private String type;
    private String venue;
    private String hostAmbassadorId;
    private Integer attendance;
    private Double pledgeTarget;
    private String status;
    private LocalDateTime eventDate;
    private LocalDateTime createdAt;
}
