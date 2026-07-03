package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "call_logs")
public class CallLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String donorId;
    private String ambassadorId;
    private String notes;
    private String outcome;
    private Instant calledAt = Instant.now();
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }
    public String getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(String ambassadorId) { this.ambassadorId = ambassadorId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Instant getCalledAt() { return calledAt; }
    public void setCalledAt(Instant calledAt) { this.calledAt = calledAt; }
}
