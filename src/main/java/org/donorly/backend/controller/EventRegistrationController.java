package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.CheckInRequest;
import org.donorly.backend.dto.RegistrationRequest;
import org.donorly.backend.model.EventRegistration;
import org.donorly.backend.service.EventRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/registrations")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @GetMapping
    @PreAuthorize("hasAuthority('events.read')")
    public List<EventRegistration> list(@PathVariable UUID eventId) {
        return registrationService.list(eventId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('events.manage')")
    public ResponseEntity<EventRegistration> register(@PathVariable UUID eventId,
                                                      @Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(registrationService.register(eventId, request));
    }

    @PostMapping("/{registrationId}/check-in")
    @PreAuthorize("hasAuthority('events.checkin')")
    public EventRegistration checkIn(@PathVariable UUID eventId, @PathVariable UUID registrationId) {
        return registrationService.checkIn(registrationId);
    }

    @PostMapping("/check-in-by-code")
    @PreAuthorize("hasAuthority('events.checkin')")
    public EventRegistration checkInByCode(@PathVariable UUID eventId, @Valid @RequestBody CheckInRequest request) {
        return registrationService.checkInByCode(eventId, request.code());
    }

    @PostMapping("/{registrationId}/cancel")
    @PreAuthorize("hasAuthority('events.manage')")
    public EventRegistration cancel(@PathVariable UUID eventId, @PathVariable UUID registrationId) {
        return registrationService.cancel(registrationId);
    }
}
