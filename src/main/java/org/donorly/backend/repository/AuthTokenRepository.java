package org.donorly.backend.repository;

import org.donorly.backend.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenAndPurpose(String token, String purpose);

    void deleteByUserIdAndPurpose(UUID userId, String purpose);
}
