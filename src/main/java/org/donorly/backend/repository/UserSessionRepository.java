package org.donorly.backend.repository;

import org.donorly.backend.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndExpiresAtBefore(UUID userId, Instant cutoff);
}
