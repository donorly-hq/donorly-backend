package org.donorly.backend.repository;

import org.donorly.backend.model.DonorActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DonorActionTokenRepository extends JpaRepository<DonorActionToken, UUID> {

    Optional<DonorActionToken> findByToken(String token);

    java.util.List<DonorActionToken> findByPledgeCardId(UUID pledgeCardId);
}
