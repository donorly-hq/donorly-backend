package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.DonorTagRequest;
import org.donorly.backend.dto.DonorTagResponse;
import org.donorly.backend.service.DonorTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/donor-tags")
@RequiredArgsConstructor
public class DonorTagController {

    private final DonorTagService tagService;

    @GetMapping
    @PreAuthorize("hasAuthority('donors.read')")
    public List<DonorTagResponse> list() {
        return tagService.listTags();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<DonorTagResponse> create(@Valid @RequestBody DonorTagRequest request) {
        return ResponseEntity.ok(tagService.createTag(request));
    }

    @DeleteMapping("/{tagId}")
    @PreAuthorize("hasAuthority('donors.write')")
    public ResponseEntity<?> delete(@PathVariable UUID tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }
}
