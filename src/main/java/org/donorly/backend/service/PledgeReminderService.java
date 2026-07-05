package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.common.BadRequestException;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.Organization;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.OrganizationRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pledge reminder emails: a daily job nags donors with unfulfilled pledges,
 * and staff can trigger a one-off reminder from the UI at any time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PledgeReminderService {

    private final PledgeRepository pledgeRepository;
    private final DonorRepository donorRepository;
    private final CampaignRepository campaignRepository;
    private final OrganizationRepository organizationRepository;
    private final CommunicationMessageRepository messageRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    /** Master switch so the scheduler can be disabled per environment. */
    @Value("${donorly.reminders.enabled:true}")
    private boolean enabled;

    /** Days between automatic reminders for the same pledge. */
    @Value("${donorly.reminders.interval-days:14}")
    private int intervalDays;

    /** Grace period after a pledge is made before the first automatic reminder. */
    @Value("${donorly.reminders.grace-days:7}")
    private int graceDays;

    /** Manual reminder for one pledge, triggered from the UI. */
    @Transactional
    public void sendReminder(UUID pledgeId) {
        UUID orgId = TenantContext.requireOrganizationId();
        Pledge pledge = pledgeRepository.findByIdAndOrganizationId(pledgeId, orgId)
                .orElseThrow(() -> new org.donorly.backend.common.NotFoundException("Pledge not found"));
        Donor donor = donorRepository.findByIdAndOrganizationId(pledge.getDonorId(), orgId)
                .orElseThrow(() -> new org.donorly.backend.common.NotFoundException("Donor not found"));
        if (donor.getEmail() == null || donor.getEmail().isBlank()) {
            throw new BadRequestException("This donor has no email address on file");
        }
        if (pledge.getCollectedAmount() != null && pledge.getAmount() != null
                && pledge.getCollectedAmount().compareTo(pledge.getAmount()) >= 0) {
            throw new BadRequestException("This pledge is already fully collected");
        }
        deliver(pledge, donor, TenantContext.getUserId());
        auditService.record("pledge.remind", "pledge", pledge.getId());
    }

    /** Daily sweep at 9:00 server time across all organizations. */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendScheduledReminders() {
        if (!enabled) {
            return;
        }
        Instant now = Instant.now();
        List<Pledge> due = pledgeRepository.findDueForReminder(
                now.minus(Duration.ofDays(graceDays)),
                now.minus(Duration.ofDays(intervalDays)));
        if (due.isEmpty()) {
            return;
        }
        log.info("Pledge reminder job: {} pledge(s) due", due.size());
        int sent = 0;
        for (Pledge pledge : due) {
            Donor donor = donorRepository.findById(pledge.getDonorId()).orElse(null);
            if (donor == null || donor.getDeletedAt() != null
                    || donor.getEmail() == null || donor.getEmail().isBlank()) {
                continue;
            }
            try {
                deliver(pledge, donor, null);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send pledge reminder for pledge {}: {}", pledge.getId(), e.getMessage());
            }
        }
        log.info("Pledge reminder job: {} reminder(s) sent", sent);
    }

    /** Builds the email, sends it, records it in message history, and stamps the pledge. */
    private void deliver(Pledge pledge, Donor donor, UUID sentBy) {
        String campaignName = campaignRepository.findById(pledge.getCampaignId())
                .map(Campaign::getName).orElse("our campaign");
        String orgName = organizationRepository.findById(pledge.getOrganizationId())
                .map(Organization::getName).orElse("Your organization");

        BigDecimal amount = pledge.getAmount() != null ? pledge.getAmount() : BigDecimal.ZERO;
        BigDecimal collected = pledge.getCollectedAmount() != null ? pledge.getCollectedAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = amount.subtract(collected).max(BigDecimal.ZERO);

        String subject = "A friendly reminder about your pledge to " + orgName;
        String body = """
                Dear %s,

                Thank you again for your generous pledge of $%s to %s (%s).

                So far we have received $%s, leaving $%s outstanding. If you have \
                already sent your gift, please disregard this note — and thank you!

                If you have any questions or would like to arrange your gift, just \
                reply to this email.

                With gratitude,
                %s
                """.formatted(
                donor.getFullName(), amount.toPlainString(), campaignName, orgName,
                collected.toPlainString(), outstanding.toPlainString(), orgName);

        emailService.sendText(donor.getEmail(), subject, body);

        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(pledge.getOrganizationId());
        message.setChannel("email");
        message.setRecipient(donor.getEmail());
        message.setDonorId(donor.getId());
        message.setSubject(subject);
        message.setBody(body);
        message.setStatus("sent");
        message.setSentBy(sentBy);
        message.setSentAt(Instant.now());
        messageRepository.save(message);

        pledge.setLastReminderAt(Instant.now());
        pledgeRepository.save(pledge);
    }
}
