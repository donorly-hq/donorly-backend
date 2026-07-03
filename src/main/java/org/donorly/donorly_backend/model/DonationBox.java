package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "donation_boxes")
public class DonationBox {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String location;
    private String assignedAmbassadorId;
    private Double totalCollected = 0.0;
    private String status = "Active";
    private Instant lastCollectedAt;
    private Instant createdAt = Instant.now();
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAssignedAmbassadorId() { return assignedAmbassadorId; }
    public void setAssignedAmbassadorId(String assignedAmbassadorId) { this.assignedAmbassadorId = assignedAmbassadorId; }
    public Double getTotalCollected() { return totalCollected; }
    public void setTotalCollected(Double totalCollected) { this.totalCollected = totalCollected; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getLastCollectedAt() { return lastCollectedAt; }
    public void setLastCollectedAt(Instant lastCollectedAt) { this.lastCollectedAt = lastCollectedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
