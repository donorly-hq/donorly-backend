package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "volunteers")
public class Volunteer {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String rowArea;
    private String status;
    private LocalDateTime createdAt;
}
