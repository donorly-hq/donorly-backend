package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "donors")
public class Donor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String email;
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
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(String ambassadorId) { this.ambassadorId = ambassadorId; }
    public Double getPledgeAmount() { return pledgeAmount; }
    public void setPledgeAmount(Double pledgeAmount) { this.pledgeAmount = pledgeAmount; }
    public Double getTotalCommitment() { return totalCommitment; }
    public void setTotalCommitment(Double totalCommitment) { this.totalCommitment = totalCommitment; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getCollected() { return collected; }
    public void setCollected(Boolean collected) { this.collected = collected; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
