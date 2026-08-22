package org.donorly.backend.repository;

import org.donorly.backend.model.PledgeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PledgeScheduleRepository extends JpaRepository<PledgeSchedule, UUID> {
    List<PledgeSchedule> findByPledgeIdAndOrganizationIdOrderByDueDateAsc(UUID pledgeId, UUID organizationId);
    List<PledgeSchedule> findByOrganizationIdAndStatusOrderByDueDateAsc(UUID organizationId, String status);
}
