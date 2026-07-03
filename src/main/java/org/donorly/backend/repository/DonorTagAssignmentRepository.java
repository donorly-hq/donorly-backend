package org.donorly.backend.repository;

import org.donorly.backend.model.DonorTagAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DonorTagAssignmentRepository extends JpaRepository<DonorTagAssignment, DonorTagAssignment.DonorTagAssignmentId> {
    List<DonorTagAssignment> findByDonorId(UUID donorId);
    void deleteByDonorIdAndTagId(UUID donorId, UUID tagId);
    boolean existsByDonorIdAndTagId(UUID donorId, UUID tagId);
}
