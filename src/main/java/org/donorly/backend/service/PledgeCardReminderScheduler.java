package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.DonorActionToken;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The automated chase loop for pledge cards. Every day it emails donors whose
 * cards are still unresolved (pending / needs_verification), spacing sends by
 * the org's reminder interval. Each email carries action links so the donor's
 * answer flows straight back into the system (see PledgeCardResponseService).
 *
 * When a card exhausts the org's attempt budget with no response, the loop
 * stops emailing, marks the card non_responsive, and opens a follow-up for the
 * card's point of contact — automation hands off to a human, never the reverse.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PledgeCardReminderScheduler {

    private static final List<String> REMINDABLE = List.of("pending", "needs_verification");

    private final PledgeCardRepository pledgeCardRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final OrganizationRepository organizationRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final FollowUpRepository followUpRepository;
    private final CommunicationMessageRepository messageRepository;
    private final DonorActionTokenService tokenService;
    private final ReminderEmailComposer composer;
    private final EmailService emailService;

    /** Master switch, same property family as pledge reminders. */
    @Value("${donorly.reminders.enabled:true}")
    private boolean enabled;

    /** Daily at 9:30 server time, after the 9:00 pledge reminder job. */
    @Scheduled(cron = "0 30 9 * * *")
    @Transactional
    public void sendPledgeCardReminders() {
        if (!enabled) {
            return;
        }
        Map<UUID, OrganizationSettings> settingsByOrg = settingsRepository.findAll().stream()
                .collect(Collectors.toMap(OrganizationSettings::getOrganizationId, Function.identity()));

        int sent = 0;
        int escalated = 0;
        for (PledgeCard card : pledgeCardRepository.findByVerificationStatusIn(REMINDABLE)) {
            OrganizationSettings settings = settingsByOrg.get(card.getOrganizationId());
            if (settings == null || !settings.isPledgeCardRemindersEnabled() || card.isRemindersPaused()) {
                continue;
            }
            Donor donor = card.getDonorId() != null
                    ? donorRepository.findById(card.getDonorId()).orElse(null) : null;
            if (donor == null || donor.getDeletedAt() != null
                    || donor.getEmail() == null || donor.getEmail().isBlank()) {
                continue;
            }

            int max = Math.max(1, settings.getPledgeCardReminderMax());
            if (card.getFollowUpCount() >= max) {
                escalated += escalate(card, donor, max) ? 1 : 0;
                continue;
            }

            Duration interval = Duration.ofDays(Math.max(1, settings.getPledgeCardReminderIntervalDays()));
            Instant cutoff = Instant.now().minus(interval);
            // First reminder waits one interval from entry; later ones from the last send.
            Instant reference = card.getLastReminderAt() != null
                    ? card.getLastReminderAt()
                    : (card.getPendingSince() != null ? card.getPendingSince() : card.getCreatedAt());
            if (reference == null || reference.isAfter(cutoff)) {
                continue;
            }

            try {
                remind(card, donor, settings);
                sent++;
            } catch (Exception e) {
                log.error("Pledge-card reminder failed for card {}: {}", card.getId(), e.getMessage());
            }
        }
        if (sent > 0 || escalated > 0) {
            log.info("Pledge-card reminder job: {} reminder(s) sent, {} card(s) escalated to a human", sent, escalated);
        }
    }

    private void remind(PledgeCard card, Donor donor, OrganizationSettings settings) {
        String orgName = organizationRepository.findById(card.getOrganizationId())
                .map(Organization::getName).orElse("Your organization");
        String campaignName = card.getCampaignId() != null
                ? campaignRepository.findById(card.getCampaignId()).map(Campaign::getName).orElse("our campaign")
                : "our campaign";
        long daysPending = card.getPendingSince() != null
                ? Duration.between(card.getPendingSince(), Instant.now()).toDays() : 0;

        DonorActionToken token = tokenService.createForCard(card);
        ReminderEmailComposer.ComposedEmail email = composer.compose(
                new ReminderEmailComposer.ReminderContext(
                        orgName, donor.getFullName(), card.getAmount(), campaignName,
                        daysPending, card.getFollowUpCount() + 1, settings.isAiEnabled()),
                token.getToken());

        emailService.sendHtml(donor.getEmail(), email.subject(), email.htmlBody());

        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(card.getOrganizationId());
        message.setChannel("email");
        message.setRecipient(donor.getEmail());
        message.setDonorId(donor.getId());
        message.setCampaignId(card.getCampaignId());
        message.setSubject(email.subject());
        message.setBody(email.htmlBody());
        message.setStatus("sent");
        message.setSentAt(Instant.now());
        messageRepository.save(message);

        card.setLastReminderAt(Instant.now());
        card.setFollowUpCount(card.getFollowUpCount() + 1);
        pledgeCardRepository.save(card);
    }

    /** Attempt budget exhausted: park the card and open a task for its POC. Returns true once. */
    private boolean escalate(PledgeCard card, Donor donor, int max) {
        if ("non_responsive".equals(card.getVerificationStatus())) {
            return false; // Already handed off.
        }
        card.setVerificationStatus("non_responsive");
        pledgeCardRepository.save(card);

        FollowUp followUp = new FollowUp();
        followUp.setOrganizationId(card.getOrganizationId());
        followUp.setDonorId(donor.getId());
        followUp.setCampaignId(card.getCampaignId());
        followUp.setAssignedToUserId(card.getPointOfContactUserId() != null
                ? card.getPointOfContactUserId() : donor.getAssignedToUserId());
        followUp.setDueAt(Instant.now().plus(Duration.ofDays(1)));
        followUp.setNotes("Donor unresponsive after " + max
                + " automated reminders — please reach out personally (pledge card "
                + (card.getAmount() != null ? "$" + card.getAmount().toPlainString() : "amount unknown") + ")");
        followUpRepository.save(followUp);
        return true;
    }
}
