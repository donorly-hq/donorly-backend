package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByStatus(String status);
    List<Event> findByType(String type);
    List<Event> findByHostAmbassadorId(String hostAmbassadorId);
}
