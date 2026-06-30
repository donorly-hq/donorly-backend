package org.donorly.donorly_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    private String password; // BCrypt hash — never store plaintext
    private String role; // "ADMIN" or "AMBASSADOR"
    private String ambassadorId;
    private Boolean active;
    private LocalDateTime createdAt;

    // --- Single-session login enforcement (Ambassador accounts) ---
    private String activeSessionToken; // jti of the currently valid JWT, null when logged out
    private LocalDateTime lastLoginAt;
}
