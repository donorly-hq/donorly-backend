package org.donorly.backend.repository;

import org.donorly.backend.model.DonorNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DonorNoteRepository extends JpaRepository<DonorNote, UUID> {
    List<DonorNote> findByOrganizationIdAndDonorIdOrderByCreatedAtDesc(UUID organizationId, UUID donorId);
}
