package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
@Entity
@Table(name = "volunteers")
public class Volunteer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String email;
    private String phone;
    private String city;
    private String ambassadorId;
    private String status = "Active";
    private String skills;
    private String availability;
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
    public String getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(String ambassadorId) { this.ambassadorId = ambassadorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
