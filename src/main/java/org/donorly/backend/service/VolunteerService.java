package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.ConflictException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.MyShiftResponse;
import org.donorly.backend.dto.ShiftRequest;
import org.donorly.backend.dto.ShiftResponse;
import org.donorly.backend.dto.VolunteerAssignRequest;
import org.donorly.backend.dto.VolunteerAssignmentResponse;
import org.donorly.backend.model.*;
import org.donorly.backend.repository.*;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerShiftRepository shiftRepository;
    private final VolunteerAssignmentRepository assignmentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditService auditService;

    // ---- Shifts ---------------------------------------------------------

    public List<ShiftResponse> listShifts(UUID eventId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireEvent(eventId, orgId);
        List<ShiftResponse> result = new ArrayList<>();
        for (VolunteerShift shift : shiftRepository.findByOrganizationIdAndEventIdOrderByStartsAtAsc(orgId, eventId)) {
            result.add(toShiftResponse(shift));
        }
        return result;
    }

    @Transactional
    public ShiftResponse createShift(UUID eventId, ShiftRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireEvent(eventId, orgId);
        VolunteerShift shift = new VolunteerShift();
        shift.setOrganizationId(orgId);
        shift.setEventId(eventId);
        apply(shift, request);
        VolunteerShift saved = shiftRepository.save(shift);
        auditService.record("volunteer.shift.create", "volunteer_shift", saved.getId());
        return toShiftResponse(saved);
    }

    @Transactional
    public ShiftResponse updateShift(UUID shiftId, ShiftRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        VolunteerShift shift = shiftRepository.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found"));
        apply(shift, request);
        VolunteerShift saved = shiftRepository.save(shift);
        auditService.record("volunteer.shift.update", "volunteer_shift", saved.getId());
        return toShiftResponse(saved);
    }

    @Transactional
    public void deleteShift(UUID shiftId) {
        UUID orgId = TenantContext.requireOrganizationId();
        VolunteerShift shift = shiftRepository.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found"));
        for (VolunteerAssignment a : assignmentRepository.findByShiftId(shiftId)) {
            assignmentRepository.delete(a);
        }
        shiftRepository.delete(shift);
        auditService.record("volunteer.shift.delete", "volunteer_shift", shiftId);
    }

    // ---- Assignments ----------------------------------------------------

    @Transactional
    public VolunteerAssignmentResponse assign(UUID shiftId, VolunteerAssignRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        VolunteerShift shift = shiftRepository.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found"));

        membershipRepository.findByOrganizationIdAndUserId(orgId, request.userId())
                .orElseThrow(() -> new BadRequestException("That user is not a member of this organization"));

        if (assignmentRepository.findByShiftIdAndUserId(shiftId, request.userId()).isPresent()) {
            throw new ConflictException("That volunteer is already assigned to this shift");
        }

        VolunteerAssignment assignment = new VolunteerAssignment();
        assignment.setOrganizationId(orgId);
        assignment.setShiftId(shiftId);
        assignment.setUserId(request.userId());
        assignment.setStatus("assigned");
        VolunteerAssignment saved = assignmentRepository.save(assignment);

        refreshShiftStatus(shift);
        auditService.record("volunteer.assign", "volunteer_shift", shiftId);
        return toAssignmentResponse(saved);
    }

    @Transactional
    public void unassign(UUID shiftId, UUID assignmentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        VolunteerShift shift = shiftRepository.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found"));
        VolunteerAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, orgId)
                .filter(a -> a.getShiftId().equals(shiftId))
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        assignmentRepository.delete(assignment);
        refreshShiftStatus(shift);
        auditService.record("volunteer.unassign", "volunteer_shift", shiftId);
    }

    @Transactional
    public VolunteerAssignmentResponse checkIn(UUID shiftId, UUID assignmentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        VolunteerAssignment assignment = assignmentRepository.findByIdAndOrganizationId(assignmentId, orgId)
                .filter(a -> a.getShiftId().equals(shiftId))
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        assignment.setStatus("checked_in");
        assignment.setCheckedInAt(Instant.now());
        VolunteerAssignment saved = assignmentRepository.save(assignment);
        auditService.record("volunteer.checkin", "volunteer_assignment", saved.getId());
        return toAssignmentResponse(saved);
    }

    public List<MyShiftResponse> myShifts() {
        UUID orgId = TenantContext.requireOrganizationId();
        UUID userId = TenantContext.getUserId();
        List<MyShiftResponse> result = new ArrayList<>();
        for (VolunteerAssignment a : assignmentRepository.findByOrganizationIdAndUserId(orgId, userId)) {
            if ("cancelled".equals(a.getStatus())) {
                continue;
            }
            VolunteerShift shift = shiftRepository.findById(a.getShiftId()).orElse(null);
            if (shift == null) {
                continue;
            }
            Event event = eventRepository.findById(shift.getEventId()).orElse(null);
            result.add(new MyShiftResponse(
                    a.getId(), a.getStatus(), a.getCheckedInAt(),
                    shift.getId(), shift.getTitle(), shift.getStartsAt(),
                    shift.getEventId(),
                    event != null ? event.getName() : null,
                    event != null ? event.getLocation() : null));
        }
        return result;
    }

    // ---- helpers --------------------------------------------------------

    private void refreshShiftStatus(VolunteerShift shift) {
        if ("cancelled".equals(shift.getStatus()) || "closed".equals(shift.getStatus())) {
            return;
        }
        long filled = assignmentRepository.countByShiftIdAndStatusNot(shift.getId(), "cancelled");
        shift.setStatus(filled >= shift.getSlots() ? "full" : "open");
        shiftRepository.save(shift);
    }

    private void apply(VolunteerShift shift, ShiftRequest request) {
        shift.setTitle(request.title());
        shift.setDescription(request.description());
        shift.setStartsAt(request.startsAt());
        shift.setEndsAt(request.endsAt());
        if (request.slots() != null && request.slots() > 0) {
            shift.setSlots(request.slots());
        }
        if (request.status() != null && !request.status().isBlank()) {
            shift.setStatus(request.status());
        }
    }

    private Event requireEvent(UUID eventId, UUID orgId) {
        return eventRepository.findByIdAndOrganizationId(eventId, orgId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private ShiftResponse toShiftResponse(VolunteerShift shift) {
        List<VolunteerAssignmentResponse> assignments = new ArrayList<>();
        for (VolunteerAssignment a : assignmentRepository.findByShiftId(shift.getId())) {
            assignments.add(toAssignmentResponse(a));
        }
        int filled = (int) assignments.stream().filter(a -> !"cancelled".equals(a.status())).count();
        return new ShiftResponse(shift.getId(), shift.getEventId(), shift.getTitle(), shift.getDescription(),
                shift.getStartsAt(), shift.getEndsAt(), shift.getSlots(), filled, shift.getStatus(), assignments);
    }

    private VolunteerAssignmentResponse toAssignmentResponse(VolunteerAssignment a) {
        String userName = userRepository.findById(a.getUserId()).map(User::getFullName).orElse(null);
        return new VolunteerAssignmentResponse(a.getId(), a.getShiftId(), a.getUserId(),
                userName, a.getStatus(), a.getCheckedInAt());
    }
}
