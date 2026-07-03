package org.donorly.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.MyShiftResponse;
import org.donorly.backend.dto.ShiftRequest;
import org.donorly.backend.dto.ShiftResponse;
import org.donorly.backend.dto.VolunteerAssignRequest;
import org.donorly.backend.dto.VolunteerAssignmentResponse;
import org.donorly.backend.service.VolunteerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    // ---- Shifts (scoped to an event) -----------------------------------

    @GetMapping("/api/events/{eventId}/shifts")
    @PreAuthorize("hasAuthority('volunteers.read')")
    public List<ShiftResponse> listShifts(@PathVariable UUID eventId) {
        return volunteerService.listShifts(eventId);
    }

    @PostMapping("/api/events/{eventId}/shifts")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public ResponseEntity<ShiftResponse> createShift(@PathVariable UUID eventId,
                                                     @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(volunteerService.createShift(eventId, request));
    }

    // ---- Shift mutations (by shift id) ---------------------------------

    @PutMapping("/api/shifts/{shiftId}")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public ShiftResponse updateShift(@PathVariable UUID shiftId, @Valid @RequestBody ShiftRequest request) {
        return volunteerService.updateShift(shiftId, request);
    }

    @DeleteMapping("/api/shifts/{shiftId}")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public ResponseEntity<?> deleteShift(@PathVariable UUID shiftId) {
        volunteerService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/shifts/{shiftId}/assignments")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public ResponseEntity<VolunteerAssignmentResponse> assign(@PathVariable UUID shiftId,
                                                              @Valid @RequestBody VolunteerAssignRequest request) {
        return ResponseEntity.ok(volunteerService.assign(shiftId, request));
    }

    @DeleteMapping("/api/shifts/{shiftId}/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public ResponseEntity<?> unassign(@PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        volunteerService.unassign(shiftId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/shifts/{shiftId}/assignments/{assignmentId}/check-in")
    @PreAuthorize("hasAuthority('volunteers.manage')")
    public VolunteerAssignmentResponse checkIn(@PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return volunteerService.checkIn(shiftId, assignmentId);
    }

    // ---- Volunteer self-service ----------------------------------------

    @GetMapping("/api/my/shifts")
    @PreAuthorize("hasAuthority('volunteers.read')")
    public List<MyShiftResponse> myShifts() {
        return volunteerService.myShifts();
    }
}
