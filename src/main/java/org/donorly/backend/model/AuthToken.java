package org.donorly.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Short-lived single-use token: password reset link or login OTP challenge. */
@Entity
@Table(name = "auth_tokens")
@Getter
@Setter
public class AuthToken {

    public static final String PURPOSE_PASSWORD_RESET = "password_reset";
    public static final String PURPOSE_LOGIN_OTP = "login_otp";
    public static final String PURPOSE_ORG_SELECT = "org_select";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false, length = 32)
    private String purpose;

    /** OTP digits (login_otp only). */
    @Column(length = 16)
    private String code;

    /** Extra state carried across the challenge, e.g. the org slug used at login. */
    @Column(length = 255)
    private String context;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
