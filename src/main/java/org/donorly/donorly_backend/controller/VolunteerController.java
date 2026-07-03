package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Volunteer;
import org.donorly.donorly_backend.service.VolunteerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping
    public List<Volunteer> getAll() {
        return volunteerService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Volunteer> getById(@PathVariable String id) {
        return volunteerService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<Volunteer> getByAmbassador(@PathVariable String ambassadorId) {
        return volunteerService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public Volunteer create(@RequestBody Volunteer volunteer) {
        return volunteerService.save(volunteer);
    }

    @PutMapping("/{id}")
    public Volunteer update(@PathVariable String id, @RequestBody Volunteer volunteer) {
        volunteer.setId(id);
        return volunteerService.save(volunteer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        volunteerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
