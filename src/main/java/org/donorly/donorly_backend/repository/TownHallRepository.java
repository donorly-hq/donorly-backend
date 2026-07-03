package org.donorly.donorly_backend.repository;

import org.donorly.donorly_backend.model.TownHall;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TownHallRepository extends JpaRepository<TownHall, String> {
    List<TownHall> findByHostAmbassadorId(String hostAmbassadorId);
}
