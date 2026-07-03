package org.donorly.backend.repository;

import org.donorly.backend.model.AiInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiInsightRepository extends JpaRepository<AiInsight, UUID> {
    List<AiInsight> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<AiInsight> findTop1ByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID organizationId, String entityType, UUID entityId);
    Optional<AiInsight> findTop1ByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(
            UUID organizationId, String entityType);
}
