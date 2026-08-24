package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.common.NotFoundException;
import org.donorly.backend.dto.DonorRespondContextResponse;
import org.donorly.backend.dto.DonorRespondRequest;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorActionToken;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorActionTokenRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

/**
 * Applies a donor's answer from a reminder email's response page. This is the
 * "capture" half of the automated chase loop: every click updates the pledge
 * card (or pledge), lands in the donor's communication history as an inbound
 * message, and creates or closes follow-ups — no staff data entry needed.
 *
 * Runs unauthenticated (the token IS the credential), so everything is scoped
 * strictly to the entities the token references.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonorResponseService {

    private final DonorActionTokenRepository tokenRepository;
    private final PledgeCardRepository pledgeCardRepository;
    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final FollowUpRepository followUpRepository;
    private final CommunicationMessageRepository messageRepository;
    private final AiGateway aiGateway;

    @Transactional(readOnly = true)
    public DonorRespondContextResponse context(String rawToken) {
        DonorActionToken token = find(rawToken);
        Donor donor = donorRepository.findById(token.getDonorId()).orElseThrow(notFound());
        String orgName = organizationRepository.findById(token.getOrganizationId())
                .map(Organization::getName).orElse("the organization");

        BigDecimal amount = null;
        UUID campaignId = null;
        if (token.getPledgeCardId() != null) {
            PledgeCard card = pledgeCardRepository.findById(token.getPledgeCardId()).orElseThrow(notFound());
            amount = card.getAmount();
            campaignId = card.getCampaignId();
        } else if (token.getPledgeId() != null) {
            Pledge pledge = pledgeRepository.findById(token.getPledgeId()).orElseThrow(notFound());
            amount = nz(pledge.getAmount()).subtract(nz(pledge.getCollectedAmount())).max(BigDecimal.ZERO);
            campaignId = pledge.getCampaignId();
        }
        String campaignName = campaignId != null
                ? campaignRepository.findById(campaignId).map(Campaign::getName).orElse(null) : null;

        return new DonorRespondContextResponse(
                orgName, firstName(donor.getFullName()), amount, campaignName,
                token.getUsedAt() != null, token.getExpiresAt().isBefore(Instant.now()));
    }

    /** Applies the donor's answer and returns a short thank-you message for the page. */
    @Transactional
    public String respond(String rawToken, DonorRespondRequest request) {
        DonorActionToken token = find(rawToken);
        if (token.getUsedAt() != null) {
            throw new BadRequestException("This link was already used. Thank you for responding!");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This link has expired. Please contact the organization directly.");
        }
        Donor donor = donorRepository.findById(token.getDonorId()).orElseThrow(notFound());

        String action = request.action() == null ? "" : request.action().toLowerCase(Locale.ROOT);
        String reply = switch (action) {
            case "paid" -> handlePaid(token, donor, request);
            case "promise" -> handlePromise(token, donor, request);
            case "stop" -> handleStop(token, donor, request);
            default -> throw new BadRequestException("Unknown action");
        };

        recordInbound(token, donor, action, request);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        return reply;
    }

    /* ── actions ─────────────────────────────────────────────── */

    private String handlePaid(DonorActionToken token, Donor donor, DonorRespondRequest request) {
        completeOpenFollowUps(token.getOrganizationId(), donor.getId(),
                "Donor confirmed payment via reminder link");
        UUID assignee = null;
        UUID campaignId = null;
        if (token.getPledgeCardId() != null) {
            PledgeCard card = pledgeCardRepository.findById(token.getPledgeCardId()).orElseThrow(notFound());
            card.setVerificationStatus("claims_paid"); // Lands in the manual verification queue.
            card.setRemindersPaused(true);
            pledgeCardRepository.save(card);
            assignee = card.getPointOfContactUserId();
            campaignId = card.getCampaignId();
        } else if (token.getPledgeId() != null) {
            Pledge pledge = pledgeRepository.findById(token.getPledgeId()).orElseThrow(notFound());
            pledge.setRemindersPaused(true);
            pledgeRepository.save(pledge);
            campaignId = pledge.getCampaignId();
        }
        createFollowUp(token, donor, campaignId, assignee,
                "Donor says they already paid — please verify and record the payment"
                        + commentSuffix(token, request),
                Instant.now().plus(Duration.ofDays(2)));
        return "Thank you! We've noted that you already sent your gift — our team will confirm it shortly.";
    }

    private String handlePromise(DonorActionToken token, Donor donor, DonorRespondRequest request) {
        LocalDate date = request.promiseDate();
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Please pick a date from today onward");
        }
        UUID assignee = null;
        UUID campaignId = null;
        if (token.getPledgeCardId() != null) {
            PledgeCard card = pledgeCardRepository.findById(token.getPledgeCardId()).orElseThrow(notFound());
            card.setPromisedDate(date);
            card.setRemindersPaused(true); // No more nagging before the promised day.
            pledgeCardRepository.save(card);
            assignee = card.getPointOfContactUserId();
            campaignId = card.getCampaignId();
        } else if (token.getPledgeId() != null) {
            Pledge pledge = pledgeRepository.findById(token.getPledgeId()).orElseThrow(notFound());
            pledge.setRemindersPaused(true);
            pledgeRepository.save(pledge);
            campaignId = pledge.getCampaignId();
        }
        createFollowUp(token, donor, campaignId, assignee,
                "Donor promised to pay by " + date + " — check in on that day"
                        + commentSuffix(token, request),
                date.atStartOfDay(ZoneOffset.UTC).toInstant());
        return "Thank you! We've noted " + date + " — we won't send further reminders before then.";
    }

    private String handleStop(DonorActionToken token, Donor donor, DonorRespondRequest request) {
        UUID assignee = null;
        UUID campaignId = null;
        if (token.getPledgeCardId() != null) {
            PledgeCard card = pledgeCardRepository.findById(token.getPledgeCardId()).orElseThrow(notFound());
            card.setRemindersPaused(true);
            pledgeCardRepository.save(card);
            assignee = card.getPointOfContactUserId();
            campaignId = card.getCampaignId();
        } else if (token.getPledgeId() != null) {
            Pledge pledge = pledgeRepository.findById(token.getPledgeId()).orElseThrow(notFound());
            pledge.setRemindersPaused(true);
            pledgeRepository.save(pledge);
            campaignId = pledge.getCampaignId();
        }
        createFollowUp(token, donor, campaignId, assignee,
                "Donor opted out of automated reminders — please decide next steps personally"
                        + commentSuffix(token, request),
                Instant.now().plus(Duration.ofDays(3)));
        return "Understood — we've stopped the automated reminders. Thank you for letting us know.";
    }

    /* ── shared plumbing ─────────────────────────────────────── */

    /** AI classification of the donor's free-text comment, appended to follow-up notes. */
    private String commentSuffix(DonorActionToken token, DonorRespondRequest request) {
        String comment = request.comment();
        if (comment == null || comment.isBlank()) {
            return "";
        }
        String suffix = ". Donor comment: \"" + comment.strip() + "\"";
        boolean aiEnabled = settingsRepository.findById(token.getOrganizationId())
                .map(org.donorly.backend.model.OrganizationSettings::isAiEnabled).orElse(false);
        if (aiEnabled && aiGateway.isEnabled()) {
            try {
                String classification = aiGateway.chat(
                        "You classify short donor messages for a fundraising team. Reply with "
                                + "exactly one line in the form: intent: <one of question|"
                                + "payment_issue|complaint|goodwill|other>; sentiment: "
                                + "<positive|neutral|negative>. Nothing else.",
                        comment.strip());
                if (classification != null && classification.length() < 120) {
                    suffix += " (AI: " + classification.strip() + ")";
                }
            } catch (Exception e) {
                log.warn("[Respond] Comment classification failed: {}", e.getMessage());
            }
        }
        return suffix;
    }

    private void completeOpenFollowUps(UUID orgId, UUID donorId, String outcome) {
        followUpRepository.findByOrganizationIdAndStatus(orgId, "open").stream()
                .filter(f -> donorId.equals(f.getDonorId()))
                .forEach(f -> {
                    f.setStatus("completed");
                    f.setOutcome(outcome);
                    followUpRepository.save(f);
                });
    }

    private void createFollowUp(DonorActionToken token, Donor donor, UUID campaignId,
                                UUID assignee, String notes, Instant dueAt) {
        FollowUp followUp = new FollowUp();
        followUp.setOrganizationId(token.getOrganizationId());
        followUp.setDonorId(donor.getId());
        followUp.setCampaignId(campaignId);
        followUp.setAssignedToUserId(assignee != null ? assignee : donor.getAssignedToUserId());
        followUp.setDueAt(dueAt);
        followUp.setNotes(notes);
        followUpRepository.save(followUp);
    }

    /** The response itself becomes an inbound message in the donor 360 history. */
    private void recordInbound(DonorActionToken token, Donor donor, String action,
                               DonorRespondRequest request) {
        String body = switch (action) {
            case "paid" -> "Donor responded via reminder link: I already paid.";
            case "promise" -> "Donor responded via reminder link: I'll pay by " + request.promiseDate() + ".";
            case "stop" -> "Donor responded via reminder link: stop the reminders.";
            default -> "Donor responded via reminder link: " + action;
        };
        if (request.comment() != null && !request.comment().isBlank()) {
            body += " Comment: \"" + request.comment().strip() + "\"";
        }
        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(token.getOrganizationId());
        message.setChannel("email");
        message.setRecipient(donor.getEmail() != null ? donor.getEmail() : "");
        message.setDonorId(donor.getId());
        message.setBody(body);
        message.setDirection("inbound");
        message.setStatus("received");
        message.setExternalId("respond:" + token.getId());
        message.setSentAt(Instant.now());
        messageRepository.save(message);
    }

    private DonorActionToken find(String rawToken) {
        return tokenRepository.findByToken(rawToken).orElseThrow(notFound());
    }

    private static java.util.function.Supplier<NotFoundException> notFound() {
        return () -> new NotFoundException("This link is not valid");
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "friend";
        return fullName.strip().split("\\s+")[0];
    }
}
