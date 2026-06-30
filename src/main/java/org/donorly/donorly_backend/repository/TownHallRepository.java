package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.TownHall;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TownHallRepository extends MongoRepository<TownHall, String> {
    List<TownHall> findByStatus(String status);
    List<TownHall> findByHostAmbassadorId(String hostAmbassadorId);
}
