package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity
@Table(name = "townhalls")
public class TownHall {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String responsiblePersonName;
    private String responsiblePersonPhone;
    private String venue;
    private String address;
    private String placeId;
    private Double lat;
    private Double lng;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private Integer durationMinutes;
    private Integer rsvpCount;
    private String hostAmbassadorId;
    private String status = "Scheduled";
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getResponsiblePersonName() { return responsiblePersonName; }
    public void setResponsiblePersonName(String v) { this.responsiblePersonName = v; }
    public String getResponsiblePersonPhone() { return responsiblePersonPhone; }
    public void setResponsiblePersonPhone(String v) { this.responsiblePersonPhone = v; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalTime getEventTime() { return eventTime; }
    public void setEventTime(LocalTime eventTime) { this.eventTime = eventTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getRsvpCount() { return rsvpCount; }
    public void setRsvpCount(Integer rsvpCount) { this.rsvpCount = rsvpCount; }
    public String getHostAmbassadorId() { return hostAmbassadorId; }
    public void setHostAmbassadorId(String hostAmbassadorId) { this.hostAmbassadorId = hostAmbassadorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
