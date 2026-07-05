package org.donorly.backend.repository;

import org.donorly.backend.model.InventoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryAssignmentRepository extends JpaRepository<InventoryAssignment, UUID> {

    List<InventoryAssignment> findByOrganizationIdAndReturnedAtIsNull(UUID organizationId);

    List<InventoryAssignment> findByItemIdAndReturnedAtIsNull(UUID itemId);

    Optional<InventoryAssignment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<InventoryAssignment> findByItemIdAndUnitNumberAndReturnedAtIsNull(UUID itemId, int unitNumber);
}
