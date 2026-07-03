package org.donorly.donorly_backend.model;
import jakarta.persistence.*;
import java.util.List;
@Entity
@Table(name = "ambassadors")
public class Ambassador {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String email;
    private String phone;
    private String city;
    private String status = "Active";
    private Double totalPledged = 0.0;
    private String parentAmbassadorId;
    @ElementCollection
    @CollectionTable(name = "ambassador_ancestor_path", joinColumns = @JoinColumn(name = "ambassador_id"))
    @Column(name = "ancestor_id")
    private List<String> ancestorPath;
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
    public Double getTotalPledged() { return totalPledged; }
    public void setTotalPledged(Double totalPledged) { this.totalPledged = totalPledged; }
    public String getParentAmbassadorId() { return parentAmbassadorId; }
    public void setParentAmbassadorId(String parentAmbassadorId) { this.parentAmbassadorId = parentAmbassadorId; }
    public List<String> getAncestorPath() { return ancestorPath; }
    public void setAncestorPath(List<String> ancestorPath) { this.ancestorPath = ancestorPath; }
}
