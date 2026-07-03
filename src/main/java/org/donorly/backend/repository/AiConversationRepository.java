package org.donorly.backend.repository;

import org.donorly.backend.model.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    List<AiConversation> findTop20ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
