package org.donorly.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "is_platform_admin", nullable = false)
    private boolean platformAdmin = false;

    @JsonIgnore
    @Column(name = "active_session_token")
    private String activeSessionToken;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
