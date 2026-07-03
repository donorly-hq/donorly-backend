package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "donation_boxes")
public class DonationBox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String location;

    private String assignedAmbassadorId;

    private Double totalCollected = 0.0;

    private String status = "Active";

    private Instant lastCollectedAt;

    private Instant createdAt = Instant.now();
}
