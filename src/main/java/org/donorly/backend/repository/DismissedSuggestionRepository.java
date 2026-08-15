package org.donorly.backend.repository;

import org.donorly.backend.model.DismissedSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DismissedSuggestionRepository extends JpaRepository<DismissedSuggestion, UUID> {
    List<DismissedSuggestion> findByOrganizationId(UUID organizationId);
    Optional<DismissedSuggestion> findByOrganizationIdAndSuggestionKey(UUID organizationId, String suggestionKey);
}
