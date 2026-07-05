package org.donorly.backend.repository;

import org.donorly.backend.model.CommunicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage, UUID> {
    List<CommunicationMessage> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    org.springframework.data.domain.Page<CommunicationMessage> findByOrganizationId(
            UUID organizationId, org.springframework.data.domain.Pageable pageable);
    List<CommunicationMessage> findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(UUID organizationId, UUID donorId);
}
