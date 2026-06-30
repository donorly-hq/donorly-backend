package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "pledge_cards")
public class PledgeCard {
    @Id
    private String id;
    private String donorId;
    private String ambassadorId;
    private String volunteerId;
    private Double amount;
    private String donationType;
    private String recurringType;
    private Integer duration;
    private String paymentMethod;
    private String cardNumber;
    private String cardExpiry;
    private String bankRouting;
    private String bankAccount;
    private String rowArea;
    private Boolean corporateMatch;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
}
