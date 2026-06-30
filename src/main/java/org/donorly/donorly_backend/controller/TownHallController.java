package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.TownHall;
import org.donorly.donorly_backend.service.TownHallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/townhalls")
@CrossOrigin(origins = "*")
public class TownHallController {

    @Autowired
    private TownHallService townHallService;

    @GetMapping
    public List<TownHall> getAll() {
        return townHallService.getAllTownHalls();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TownHall> getById(@PathVariable String id) {
        return townHallService.getTownHallById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-ambassador/{ambassadorId}")
    public List<TownHall> getByAmbassador(@PathVariable String ambassadorId) {
        return townHallService.getByHostAmbassador(ambassadorId);
    }

    @PostMapping
    public TownHall create(@RequestBody TownHall townHall) {
        return townHallService.createTownHall(townHall);
    }

    @PutMapping("/{id}")
    public TownHall update(@PathVariable String id, @RequestBody TownHall townHall) {
        return townHallService.updateTownHall(id, townHall);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        townHallService.deleteTownHall(id);
        return ResponseEntity.ok().build();
    }
}
