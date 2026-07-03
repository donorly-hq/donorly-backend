package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.service.DonorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<Donor> getById(@PathVariable UUID id) {
        return donorService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<Donor> getByAmbassador(@PathVariable UUID ambassadorId) {
        return donorService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public Donor create(@RequestBody Donor donor) {
        return donorService.save(donor);
    }

    @PutMapping("/{id}")
    public Donor update(@PathVariable UUID id, @RequestBody Donor donor) {
        donor.setDonorId(id);
        return donorService.save(donor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
