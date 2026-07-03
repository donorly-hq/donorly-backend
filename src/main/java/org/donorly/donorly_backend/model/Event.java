package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity
@Table(name = "events")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private String ambassadorId;
    private String status = "Upcoming";
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalTime getEventTime() { return eventTime; }
    public void setEventTime(LocalTime eventTime) { this.eventTime = eventTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAmbassadorId() { return ambassadorId; }
    public void setAmbassadorId(String ambassadorId) { this.ambassadorId = ambassadorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
