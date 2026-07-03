package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.PledgeCard;
import org.donorly.donorly_backend.service.PledgeCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pledge-cards")
@RequiredArgsConstructor
public class PledgeCardController {

    private final PledgeCardService pledgeCardService;

    @GetMapping
    public List<PledgeCard> getAll() {
        return pledgeCardService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PledgeCard> getById(@PathVariable String id) {
        return pledgeCardService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/donor/{donorId}")
    public List<PledgeCard> getByDonor(@PathVariable String donorId) {
        return pledgeCardService.getByDonorId(donorId);
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<PledgeCard> getByAmbassador(@PathVariable String ambassadorId) {
        return pledgeCardService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public PledgeCard create(@RequestBody PledgeCard pledgeCard) {
        return pledgeCardService.save(pledgeCard);
    }

    @PutMapping("/{id}")
    public PledgeCard update(@PathVariable String id, @RequestBody PledgeCard pledgeCard) {
        pledgeCard.setId(id);
        return pledgeCardService.save(pledgeCard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        pledgeCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
