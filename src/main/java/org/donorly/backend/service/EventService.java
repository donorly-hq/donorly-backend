package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.EventRequest;
import org.donorly.backend.dto.EventSummaryResponse;
import org.donorly.backend.model.Event;
import org.donorly.backend.model.VolunteerShift;
import org.donorly.backend.repository.EventRegistrationRepository;
import org.donorly.backend.repository.EventRepository;
import org.donorly.backend.repository.VolunteerAssignmentRepository;
import org.donorly.backend.repository.VolunteerShiftRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final VolunteerShiftRepository shiftRepository;
    private final VolunteerAssignmentRepository assignmentRepository;
    private final AuditService auditService;

    public List<Event> list() {
        return eventRepository.findByOrganizationIdOrderByStartsAtDesc(TenantContext.requireOrganizationId());
    }

    public Event get(UUID id) {
        return eventRepository.findByIdAndOrganizationId(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional
    public Event create(EventRequest request) {
        Event event = new Event();
        event.setOrganizationId(TenantContext.requireOrganizationId());
        apply(event, request);
        Event saved = eventRepository.save(event);
        auditService.record("event.create", "event", saved.getId());
        return saved;
    }

    @Transactional
    public Event update(UUID id, EventRequest request) {
        Event event = get(id);
        apply(event, request);
        Event saved = eventRepository.save(event);
        auditService.record("event.update", "event", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Event event = get(id);
        eventRepository.delete(event);
        auditService.record("event.delete", "event", id);
    }

    public EventSummaryResponse summary(UUID id) {
        Event event = get(id);
        long registrations = registrationRepository.countByEventId(id);
        long checkedIn = registrationRepository.countByEventIdAndStatus(id, "checked_in");

        long totalGuests = 0;
        for (var reg : registrationRepository.findByOrganizationIdAndEventIdOrderByGuestNameAsc(
                event.getOrganizationId(), id)) {
            if (!"cancelled".equals(reg.getStatus())) {
                totalGuests += reg.getPartySize() != null ? reg.getPartySize() : 1;
            }
        }

        List<VolunteerShift> shifts = shiftRepository.findByOrganizationIdAndEventIdOrderByStartsAtAsc(
                event.getOrganizationId(), id);
        long slots = 0;
        long filled = 0;
        for (VolunteerShift shift : shifts) {
            slots += shift.getSlots() != null ? shift.getSlots() : 0;
            filled += assignmentRepository.countByShiftIdAndStatusNot(shift.getId(), "cancelled");
        }

        return new EventSummaryResponse(event.getId(), event.getName(), event.getStatus(),
                event.getCapacity(), registrations, checkedIn, totalGuests,
                shifts.size(), slots, filled);
    }

    private void apply(Event event, EventRequest request) {
        event.setName(request.name());
        event.setDescription(request.description());
        if (request.eventType() != null) {
            event.setEventType(request.eventType());
        }
        event.setLocation(request.location());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCapacity(request.capacity());
        if (request.status() != null) {
            event.setStatus(request.status());
        }
        event.setCampaignId(request.campaignId());
    }
}
