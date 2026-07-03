package org.donorly.donorly_backend.controller;
import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.DonationBox;
import org.donorly.donorly_backend.service.DonationBoxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/donation-boxes")
@RequiredArgsConstructor
public class DonationBoxController {
    private final DonationBoxService donationBoxService;
    @GetMapping public List<DonationBox> getAll() { return donationBoxService.getAll(); }
    @GetMapping("/{id}") public ResponseEntity<DonationBox> getById(@PathVariable String id) { return donationBoxService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @GetMapping("/ambassador/{ambassadorId}") public List<DonationBox> getByAmbassador(@PathVariable String ambassadorId) { return donationBoxService.getByAmbassadorId(ambassadorId); }
    @PostMapping public DonationBox create(@RequestBody DonationBox donationBox) { return donationBoxService.save(donationBox); }
    @PutMapping("/{id}") public DonationBox update(@PathVariable String id, @RequestBody DonationBox donationBox) { donationBox.setId(id); return donationBoxService.save(donationBox); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id) { donationBoxService.delete(id); return ResponseEntity.noContent().build(); }
}
