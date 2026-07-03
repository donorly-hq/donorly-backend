package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.EventRequest;
import org.donorly.backend.dto.EventSummaryResponse;
import org.donorly.backend.model.Event;
import org.donorly.backend.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @PreAuthorize("hasAuthority('events.read')")
    public List<Event> list() {
        return eventService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('events.read')")
    public Event get(@PathVariable UUID id) {
        return eventService.get(id);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('events.read')")
    public EventSummaryResponse summary(@PathVariable UUID id) {
        return eventService.summary(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('events.manage')")
    public ResponseEntity<Event> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('events.manage')")
    public Event update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('events.manage')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
