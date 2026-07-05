package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.PublicCheckinInfo;
import org.donorly.backend.dto.PublicThermometerResponse;
import org.donorly.backend.model.AuditLog;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Event;
import org.donorly.backend.model.EventRegistration;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.AuditLogRepository;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.EventRegistrationRepository;
import org.donorly.backend.repository.EventRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unauthenticated flows: the projector thermometer (data is anonymized)
 * and QR self-check-in (authorized by the unguessable check-in code).
 */
@Service
@RequiredArgsConstructor
public class PublicPortalService {

    private final OrganizationRepository organizationRepository;
    private final CampaignRepository campaignRepository;
    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final AuditLogRepository auditLogRepository;

    /** Keyed by campaign UUID — an unguessable capability URL, no login needed. */
    public PublicThermometerResponse thermometer(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        Organization org = organizationRepository.findById(campaign.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        BigDecimal pledged = pledgeRepository.sumPledgedByCampaign(org.getId(), campaign.getId());
        BigDecimal collected = pledgeRepository.sumCollectedByCampaign(org.getId(), campaign.getId());
        int count = pledgeRepository.findByOrganizationIdAndCampaignId(org.getId(), campaign.getId()).size();

        var recent = pledgeRepository
                .findTop10ByOrganizationIdAndCampaignIdOrderByCreatedAtDesc(org.getId(), campaign.getId());
        var donorNames = donorRepository.findAllById(
                        recent.stream().map(Pledge::getDonorId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Donor::getId, Donor::getFullName));

        var recentDtos = recent.stream()
                .map(p -> new PublicThermometerResponse.RecentPledge(
                        abbreviate(donorNames.get(p.getDonorId())),
                        p.getAmount(), p.getCreatedAt()))
                .toList();

        return new PublicThermometerResponse(
                org.getName(), campaign.getName(), campaign.getGoalAmount(),
                pledged != null ? pledged : BigDecimal.ZERO,
                collected != null ? collected : BigDecimal.ZERO,
                count, recentDtos);
    }

    public PublicCheckinInfo checkinInfo(UUID eventId, String code) {
        EventRegistration reg = findRegistration(eventId, code);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return toInfo(event, reg);
    }

    @Transactional
    public PublicCheckinInfo selfCheckIn(UUID eventId, String code) {
        EventRegistration reg = findRegistration(eventId, code);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if ("cancelled".equals(reg.getStatus())) {
            throw new BadRequestException("This registration was cancelled");
        }
        if (reg.getCheckedInAt() == null) {
            reg.setStatus("checked_in");
            reg.setCheckedInAt(Instant.now());
            // checkedInBy stays null — self-service check-in
            registrationRepository.save(reg);

            AuditLog log = new AuditLog();
            log.setOrganizationId(reg.getOrganizationId());
            log.setAction("event.self_checkin");
            log.setEntityType("event_registration");
            log.setEntityId(reg.getId());
            auditLogRepository.save(log);
        }
        return toInfo(event, reg);
    }

    private EventRegistration findRegistration(UUID eventId, String code) {
        return registrationRepository
                .findByEventIdAndCheckInCode(eventId, code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("No registration matches that code"));
    }

    private static PublicCheckinInfo toInfo(Event event, EventRegistration reg) {
        return new PublicCheckinInfo(
                event.getName(), event.getLocation(), event.getStartsAt(),
                reg.getGuestName(),
                reg.getPartySize() != null ? reg.getPartySize() : 1,
                reg.getStatus(), reg.getCheckedInAt());
    }

    /** "Ahmed Hassan" → "Ahmed H." — enough for a public screen. */
    static String abbreviate(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Anonymous";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
