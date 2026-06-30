package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.TownHall;
import org.donorly.donorly_backend.repository.TownHallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TownHallService {

    @Autowired
    private TownHallRepository townHallRepository;

    public List<TownHall> getAllTownHalls() {
        return townHallRepository.findAll();
    }

    public Optional<TownHall> getTownHallById(String id) {
        return townHallRepository.findById(id);
    }

    public List<TownHall> getByHostAmbassador(String ambassadorId) {
        return townHallRepository.findByHostAmbassadorId(ambassadorId);
    }

    public TownHall createTownHall(TownHall townHall) {
        townHall.setCreatedAt(LocalDateTime.now());
        if (townHall.getStatus() == null) {
            townHall.setStatus("planned");
        }
        return townHallRepository.save(townHall);
    }

    public TownHall updateTownHall(String id, TownHall updated) {
        updated.setId(id);
        return townHallRepository.save(updated);
    }

    public void deleteTownHall(String id) {
        townHallRepository.deleteById(id);
    }
}
