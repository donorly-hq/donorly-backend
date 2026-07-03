package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "pledge_cards")
public class PledgeCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String donorId;

    private String ambassadorId;

    private Double amount = 0.0;

    private String paymentMethod;

    private String status = "Pledged"; // Pledged or Collected

    private String notes;

    private Instant createdAt = Instant.now();
}
