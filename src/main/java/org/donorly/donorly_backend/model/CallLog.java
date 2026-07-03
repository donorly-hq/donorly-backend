package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "call_logs")
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String donorId;

    private String ambassadorId;

    private String notes;

    private String outcome; // e.g. Answered, No Answer, Callback

    private Instant calledAt = Instant.now();
}
