package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByAmbassadorId(String ambassadorId);
}
