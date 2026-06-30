package org.donorly.donorly_backend.controller;

import org.donorly.donorly_backend.model.PledgeCard;
import org.donorly.donorly_backend.service.PledgeCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pledge-cards")
@CrossOrigin(origins = "*")
public class PledgeCardController {

    @Autowired
    private PledgeCardService pledgeCardService;

    @GetMapping
    public List<PledgeCard> getAll() {
        return pledgeCardService.getAllPledgeCards();
    }

    @GetMapping("/unprocessed")
    public List<PledgeCard> getUnprocessed() {
        return pledgeCardService.getUnprocessedCards();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PledgeCard> getById(@PathVariable String id) {
        return pledgeCardService.getPledgeCardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PledgeCard create(@RequestBody PledgeCard pledgeCard) {
        return pledgeCardService.createPledgeCard(pledgeCard);
    }

    @PutMapping("/{id}")
    public PledgeCard update(@PathVariable String id, @RequestBody PledgeCard pledgeCard) {
        return pledgeCardService.updatePledgeCard(id, pledgeCard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        pledgeCardService.deletePledgeCard(id);
        return ResponseEntity.ok().build();
    }
}
