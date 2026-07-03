package org.donorly.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "donor_tag_assignments")
@IdClass(DonorTagAssignment.DonorTagAssignmentId.class)
@Getter
@Setter
public class DonorTagAssignment {

    @Id
    @Column(name = "donor_id")
    private UUID donorId;

    @Id
    @Column(name = "tag_id")
    private UUID tagId;

    @Getter
    @Setter
    public static class DonorTagAssignmentId implements Serializable {
        private UUID donorId;
        private UUID tagId;
    }
}
