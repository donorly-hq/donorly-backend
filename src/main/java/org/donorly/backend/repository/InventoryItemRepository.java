package org.donorly.backend.repository;

import org.donorly.backend.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<InventoryItem> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
