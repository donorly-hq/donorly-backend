package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "donors")
public class Donor {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Double pledgeAmount;
    private String donationType;
    private String recurringType;
    private Integer duration;
    private String paymentMethod;
    private String status;
    private String ambassadorId;
    private Boolean corporateMatch;
    private String employer;
    private LocalDateTime createdAt;
}
