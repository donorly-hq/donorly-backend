package org.donorly.backend.repository;

import org.donorly.backend.model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
    List<MessageTemplate> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<MessageTemplate> findByIdAndOrganizationId(UUID id, UUID organizationId);
    long countByOrganizationId(UUID organizationId);
}
