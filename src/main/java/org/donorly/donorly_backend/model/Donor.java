package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    private String city;

    private String status = "Active";

    private String ambassadorId;

    private Double pledgeAmount = 0.0;

    private Double totalCommitment = 0.0;

    private String paymentMethod;

    private String message;

    private Boolean collected = false;

    private Instant createdAt = Instant.now();
}
