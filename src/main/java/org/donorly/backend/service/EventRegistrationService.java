package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.RegistrationRequest;
import org.donorly.backend.model.Event;
import org.donorly.backend.model.EventRegistration;
import org.donorly.backend.repository.EventRegistrationRepository;
import org.donorly.backend.repository.EventRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public List<EventRegistration> list(UUID eventId) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireEvent(eventId, orgId);
        return registrationRepository.findByOrganizationIdAndEventIdOrderByGuestNameAsc(orgId, eventId);
    }

    @Transactional
    public EventRegistration register(UUID eventId, RegistrationRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireEvent(eventId, orgId);

        EventRegistration reg = new EventRegistration();
        reg.setOrganizationId(orgId);
        reg.setEventId(eventId);
        reg.setDonorId(request.donorId());
        reg.setGuestName(request.guestName());
        reg.setGuestEmail(request.guestEmail());
        reg.setGuestPhone(request.guestPhone());
        reg.setPartySize(request.partySize() != null && request.partySize() > 0 ? request.partySize() : 1);
        reg.setNotes(request.notes());
        reg.setStatus("registered");
        reg.setCheckInCode(generateUniqueCode(eventId));
        EventRegistration saved = registrationRepository.save(reg);
        auditService.record("event.register", "event_registration", saved.getId());
        return saved;
    }

    @Transactional
    public EventRegistration checkIn(UUID registrationId) {
        UUID orgId = TenantContext.requireOrganizationId();
        EventRegistration reg = registrationRepository.findByIdAndOrganizationId(registrationId, orgId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        return applyCheckIn(reg);
    }

    @Transactional
    public EventRegistration checkInByCode(UUID eventId, String code) {
        UUID orgId = TenantContext.requireOrganizationId();
        requireEvent(eventId, orgId);
        EventRegistration reg = registrationRepository
                .findByEventIdAndCheckInCode(eventId, code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("No registration matches that code"));
        return applyCheckIn(reg);
    }

    @Transactional
    public EventRegistration cancel(UUID registrationId) {
        UUID orgId = TenantContext.requireOrganizationId();
        EventRegistration reg = registrationRepository.findByIdAndOrganizationId(registrationId, orgId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        reg.setStatus("cancelled");
        EventRegistration saved = registrationRepository.save(reg);
        auditService.record("event.registration.cancel", "event_registration", saved.getId());
        return saved;
    }

    private EventRegistration applyCheckIn(EventRegistration reg) {
        if ("cancelled".equals(reg.getStatus())) {
            throw new BadRequestException("This registration was cancelled");
        }
        reg.setStatus("checked_in");
        reg.setCheckedInAt(Instant.now());
        reg.setCheckedInBy(TenantContext.getUserId());
        EventRegistration saved = registrationRepository.save(reg);
        auditService.record("event.checkin", "event_registration", saved.getId());
        return saved;
    }

    private Event requireEvent(UUID eventId, UUID orgId) {
        return eventRepository.findByIdAndOrganizationId(eventId, orgId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private String generateUniqueCode(UUID eventId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (registrationRepository.findByEventIdAndCheckInCode(eventId, code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique check-in code");
    }
}
