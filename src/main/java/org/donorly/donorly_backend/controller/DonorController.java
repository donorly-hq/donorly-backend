package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.dto.DonorCreateRequest;
import org.donorly.donorly_backend.dto.DonorResponseDto;
import org.donorly.donorly_backend.service.DonorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;

    @GetMapping
    public List<DonorResponseDto> getAll() {
        return donorService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonorResponseDto> getById(@PathVariable UUID id) {
        return donorService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<DonorResponseDto> getByAmbassador(@PathVariable UUID ambassadorId) {
        return donorService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public DonorResponseDto create(@RequestBody DonorCreateRequest request) {
        return donorService.create(request);
    }

    // Frontend sends the full donor object back on update; we only
    // actually need the status field to update the underlying pledge.
    @PutMapping("/{id}")
    public DonorResponseDto update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String status = body.get("status") == null ? null : body.get("status").toString();
        return donorService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
