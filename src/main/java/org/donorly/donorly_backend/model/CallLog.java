package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "call_logs")
public class CallLog {
    @Id
    private String id;
    private String pledgeCardId;
    private String donorId;
    private String callerId;
    private String outcome;
    private String notes;
    private Integer attemptCount;
    private LocalDateTime calledAt;
}
