package org.donorly.donorly_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ambassadors")
public class Ambassador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    private String city;

    private String status = "Active";

    private Double totalPledged = 0.0;

    private String parentAmbassadorId;

    @ElementCollection
    @CollectionTable(name = "ambassador_ancestor_path", joinColumns = @JoinColumn(name = "ambassador_id"))
    @Column(name = "ancestor_id")
    private List<String> ancestorPath;
}
