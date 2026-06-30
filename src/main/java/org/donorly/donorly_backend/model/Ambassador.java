package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "ambassadors")
public class Ambassador {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String code;
    private Double pledgeGoal;
    private Double totalPledged;
    private String status;
    private LocalDateTime createdAt;

    // --- Ambassador hierarchy ---
    // The ambassador who created this one. Null for root-level ambassadors
    // (root-level ambassadors can currently only be created by Admin).
    private String parentAmbassadorId;

    // Full chain of ancestor IDs, root-first, NOT including this ambassador's own id.
    // e.g. if A created B and B created C, then C.ancestorPath = [A.id, B.id]
    // Lets "find all descendants of X" be a single indexed query
    // (ancestorPath contains X) instead of a recursive walk.
    private List<String> ancestorPath = new ArrayList<>();
}
