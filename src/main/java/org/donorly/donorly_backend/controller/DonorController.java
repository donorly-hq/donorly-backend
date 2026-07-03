package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.service.DonorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;

    @GetMapping
    public List<Donor> getAll() {
        return donorService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Donor> getById(@PathVariable String id) {
        return donorService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<Donor> getByAmbassador(@PathVariable String ambassadorId) {
        return donorService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public Donor create(@RequestBody Donor donor) {
        return donorService.save(donor);
    }

    @PutMapping("/{id}")
    public Donor update(@PathVariable String id, @RequestBody Donor donor) {
        donor.setId(id);
        return donorService.save(donor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
