package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.DonationBox;
import org.donorly.donorly_backend.service.DonationBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/donation-boxes")
@CrossOrigin(origins = "*")
public class DonationBoxController {

    @Autowired
    private DonationBoxService donationBoxService;

    @GetMapping
    public List<DonationBox> getAll() {
        return donationBoxService.getAllBoxes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationBox> getById(@PathVariable String id) {
        return donationBoxService.getBoxById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public List<DonationBox> getByStatus(@PathVariable String status) {
        return donationBoxService.getBoxesByStatus(status);
    }

    @PostMapping
    public DonationBox create(@RequestBody DonationBox box) {
        return donationBoxService.createBox(box);
    }

    @PutMapping("/{id}")
    public DonationBox update(@PathVariable String id, @RequestBody DonationBox box) {
        return donationBoxService.updateBox(id, box);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        donationBoxService.deleteBox(id);
        return ResponseEntity.ok().build();
    }
}
