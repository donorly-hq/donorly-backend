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
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String donorId;
    private String ambassadorId;
    private String notes;
    private String outcome;
    private Instant calledAt = Instant.now();
}
