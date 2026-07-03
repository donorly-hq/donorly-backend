package org.donorly.donorly_backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Event;
import org.donorly.donorly_backend.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<Event> getAll() {
        return eventService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable String id) {
        return eventService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ambassador/{ambassadorId}")
    public List<Event> getByAmbassador(@PathVariable String ambassadorId) {
        return eventService.getByAmbassadorId(ambassadorId);
    }

    @PostMapping
    public Event create(@RequestBody Event event) {
        return eventService.save(event);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable String id, @RequestBody Event event) {
        event.setId(id);
        return eventService.save(event);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
