package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "donation_boxes")
public class DonationBox {
    @Id
    private String id;
    private String boxNumber;
    private String area;
    private String volunteerId;
    private Double amount;
    private String status;
    private LocalDateTime createdAt;
}
