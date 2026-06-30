package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Volunteer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VolunteerRepository extends MongoRepository<Volunteer, String> {
    List<Volunteer> findByRole(String role);
    List<Volunteer> findByStatus(String status);
    List<Volunteer> findByRowArea(String rowArea);
}
