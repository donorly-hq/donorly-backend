package org.donorly.backend.repository;

import org.donorly.backend.model.CampaignMessaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignMessagingRepository extends JpaRepository<CampaignMessaging, UUID> {
    Optional<CampaignMessaging> findByCampaignIdAndOrganizationId(UUID campaignId, UUID organizationId);
    List<CampaignMessaging> findByOrganizationId(UUID organizationId);
}
