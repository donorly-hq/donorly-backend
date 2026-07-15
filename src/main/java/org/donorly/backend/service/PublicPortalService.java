package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.PublicCheckinInfo;
import org.donorly.backend.dto.PublicSelfPledgeRequest;
import org.donorly.backend.dto.PublicSelfPledgeResponse;
import org.donorly.backend.dto.PublicThermometerResponse;
import org.donorly.backend.model.AuditLog;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Event;
import org.donorly.backend.model.EventRegistration;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeStatus;
import org.donorly.backend.repository.AuditLogRepository;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.EventRegistrationRepository;
import org.donorly.backend.repository.EventRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.security.LoginRateLimiter;
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
    private final LoginRateLimiter rateLimiter;
    private final DonorMatchingService donorMatchingService;
    private final CampaignProgressService campaignProgressService;

    private static final BigDecimal SELF_PLEDGE_MAX = new BigDecimal("100000");

    /** Keyed by campaign UUID — an unguessable capability URL, no login needed. */
    public PublicThermometerResponse thermometer(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        Organization org = organizationRepository.findById(campaign.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        var progress = campaignProgressService.progress(org.getId(), campaign.getId());

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
                progress.pledged(), progress.collected(),
                progress.pledgeCount(), recentDtos);
    }

    /**
     * Unauthenticated pledge from a donor's own phone (Event Mode QR).
     * Guards: campaign must be active, amount is capped, and submissions are
     * rate-limited per client IP. The key is bucketed per minute so a venue
     * NAT (one IP for the whole room) still gets 5 pledges/minute rather
     * than 5 per quarter hour. Pledges land as source="self" for staff review.
     */
    @Transactional
    public PublicSelfPledgeResponse selfPledge(UUID campaignId, PublicSelfPledgeRequest request, String clientIp) {
        String limiterKey = "selfpledge:" + clientIp + ":" + (Instant.now().getEpochSecond() / 60);
        if (rateLimiter.isBlocked(limiterKey)) {
            throw new BadRequestException("Too many pledges from this connection — please try again in a minute.");
        }
        rateLimiter.recordFailure(limiterKey);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        if (!"active".equals(campaign.getStatus())) {
            throw new BadRequestException("This campaign is not accepting pledges right now");
        }
        Organization org = organizationRepository.findById(campaign.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (request.amount().compareTo(SELF_PLEDGE_MAX) > 0) {
            throw new BadRequestException("For pledges this large, please speak with a volunteer");
        }

        String name = request.fullName().trim();
        String email = blankToNull(request.email());
        String phone = blankToNull(request.phone());

        Donor donor = donorMatchingService.findExistingDonor(org.getId(), name, email, phone);
        if (donor == null) {
            donor = new Donor();
            donor.setOrganizationId(org.getId());
            donor.setFullName(name);
            donor.setEmail(email);
            donor.setPhone(phone);
            donor = donorRepository.save(donor);
        }

        Pledge pledge = new Pledge();
        pledge.setOrganizationId(org.getId());
        pledge.setCampaignId(campaign.getId());
        pledge.setDonorId(donor.getId());
        pledge.setAmount(request.amount());
        pledge.setStartDate(java.time.LocalDate.now());
        pledge.setSource("self");
        pledge.setStatus(PledgeStatus.PENDING.value());
        Pledge saved = pledgeRepository.save(pledge);

        AuditLog log = new AuditLog();
        log.setOrganizationId(org.getId());
        log.setAction("pledge.self_create");
        log.setEntityType("pledge");
        log.setEntityId(saved.getId());
        auditLogRepository.save(log);

        return new PublicSelfPledgeResponse(
                donor.getFullName(), saved.getAmount(), campaign.getName(), org.getName());
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public PublicCheckinInfo checkinInfo(UUID eventId, String code, String clientIp) {
        checkCheckinRate(clientIp);
        EventRegistration reg = findRegistration(eventId, code);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return toInfo(event, reg);
    }

    @Transactional
    public PublicCheckinInfo selfCheckIn(UUID eventId, String code, String clientIp) {
        checkCheckinRate(clientIp);
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

    /**
     * Check-in codes are 8-char capabilities; throttle per client IP so they cannot be
     * brute-forced. Keyed per minute like self-pledge, so a busy kiosk recovers quickly.
     */
    private void checkCheckinRate(String clientIp) {
        String limiterKey = "checkin:" + clientIp + ":" + (Instant.now().getEpochSecond() / 60);
        if (rateLimiter.isBlocked(limiterKey)) {
            throw new BadRequestException("Too many attempts from this connection — please wait a minute.");
        }
        rateLimiter.recordFailure(limiterKey);
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
