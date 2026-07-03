package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.TownHall;
import org.donorly.donorly_backend.service.TownHallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/townhalls")
@RequiredArgsConstructor
public class TownHallController {

    private final TownHallService townHallService;

    @GetMapping
    public List<TownHall> getAll() {
        return townHallService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TownHall> getById(@PathVariable String id) {
        return townHallService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<TownHall> getByAmbassador(@PathVariable String ambassadorId) {
        return townHallService.getByAmbassador(ambassadorId);
    }

    @PostMapping
    public TownHall create(@RequestBody TownHall townHall) {
        return townHallService.save(townHall);
    }

    @PutMapping("/{id}")
    public TownHall update(@PathVariable String id, @RequestBody TownHall townHall) {
        townHall.setId(id);
        return townHallService.save(townHall);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        townHallService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
