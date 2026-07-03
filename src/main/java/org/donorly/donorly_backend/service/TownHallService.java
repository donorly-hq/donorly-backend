package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.TownHall;
import org.donorly.donorly_backend.repository.TownHallRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TownHallService {

    private final TownHallRepository townHallRepository;

    public List<TownHall> getAll() {
        return townHallRepository.findAll();
    }

    public Optional<TownHall> getById(String id) {
        return townHallRepository.findById(id);
    }

    public TownHall save(TownHall townHall) {
        return townHallRepository.save(townHall);
    }

    public void delete(String id) {
        townHallRepository.deleteById(id);
    }

    public List<TownHall> getByAmbassador(String ambassadorId) {
        return townHallRepository.findByHostAmbassadorId(ambassadorId);
    }
}
