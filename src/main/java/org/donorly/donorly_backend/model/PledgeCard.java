package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "pledge_cards")
public class PledgeCard {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String donorId;
    private String ambassadorId;
    private Double amount = 0.0;
    private String paymentMethod;
    private String status = "Pledged";
    private String notes;
    private Instant createdAt = Instant.now();
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }
    public String getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(String ambassadorId) { this.ambassadorId = ambassadorId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
