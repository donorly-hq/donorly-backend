package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VolunteerRepository extends JpaRepository<Volunteer, String> {
    List<Volunteer> findByAmbassadorId(String ambassadorId);
}
