package org.donorly.backend.repository;

import org.donorly.backend.model.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {
}
