package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.model.FollowUp;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeSchedule;
import org.donorly.backend.repository.CampaignMessagingRepository;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorRepository;
import org.donorly.backend.repository.FollowUpRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.donorly.backend.repository.PledgeRepository;
import org.donorly.backend.repository.PledgeScheduleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Interactive replies from donors over SMS/WhatsApp (Twilio inbound webhook):
 * <ul>
 *   <li><b>1</b> — ready to pay: replies with the payment link and flags the pledge
 *       via a follow-up for the donor's point of contact.</li>
 *   <li><b>2</b> — needs time: bumps the follow-up count and schedules a check-in
 *       in a week.</li>
 *   <li><b>3</b> — wants a human: creates an urgent follow-up for the POC.</li>
 *   <li><b>"installment"/"plan"</b> — drafts a monthly installment schedule for
 *       the donor's latest outstanding pledge.</li>
 * </ul>
 * Every inbound message is recorded in the communication history (direction
 * {@code inbound}) so the donor 360 page shows both sides of the conversation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioInboundService {

    private static final int DEFAULT_INSTALLMENTS = 4;

    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;
    private final PledgeCardRepository pledgeCardRepository;
    private final PledgeScheduleRepository pledgeScheduleRepository;
    private final FollowUpRepository followUpRepository;
    private final CommunicationMessageRepository messageRepository;
    private final CampaignMessagingRepository campaignMessagingRepository;

    @Value("${donorly.mail.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    /** Handles one inbound message and returns the reply text (sent back as TwiML). */
    @Transactional
    public String handleInbound(String from, String body, String messageSid) {
        boolean whatsapp = from != null && from.startsWith("whatsapp:");
        String phone = whatsapp ? from.substring("whatsapp:".length()) : from;
        String text = body == null ? "" : body.trim();

        Donor donor = matchDonor(phone);
        recordInbound(donor, whatsapp ? "whatsapp" : "sms", phone, text, messageSid);

        if (donor == null) {
            log.info("[Twilio-inbound] No donor matched inbound number — generic reply sent");
            return "JazakAllah khair for your message. A volunteer will get back to you soon.";
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("installment") || lower.contains("plan")) {
            return createInstallmentDraft(donor);
        }
        return switch (lower) {
            case "1" -> readyToPay(donor);
            case "2" -> needsTime(donor);
            case "3" -> wantsHuman(donor);
            default -> "Thank you! Reply 1 to get the payment link, 2 if you need more time, "
                    + "3 to talk to someone, or \"installments\" to split your pledge.";
        };
    }

    private Donor matchDonor(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("\\D", "");
        List<Donor> matches = donorRepository.findByPhoneDigits(digits);
        if (matches.isEmpty() && digits.length() == 11 && digits.startsWith("1")) {
            // US numbers arrive as +1XXXXXXXXXX but are often stored without country code.
            matches = donorRepository.findByPhoneDigits(digits.substring(1));
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String readyToPay(Donor donor) {
        Pledge pledge = latestOutstandingPledge(donor);
        String link = null;
        if (pledge != null && pledge.getCampaignId() != null) {
            link = campaignMessagingRepository.findById(pledge.getCampaignId())
                    .map(m -> m.getPaymentLink())
                    .filter(l -> l != null && !l.isBlank())
                    .orElse(appBaseUrl + "/p/" + pledge.getCampaignId());
        }
        createFollowUp(donor, pledge, "Donor replied READY TO PAY (1) — send/confirm payment",
                Duration.ofDays(1));
        if (link != null) {
            return "JazakAllah khair, " + donor.getFullName() + "! You can pay here: " + link;
        }
        return "JazakAllah khair, " + donor.getFullName() + "! A volunteer will contact you with payment details.";
    }

    private String needsTime(Donor donor) {
        bumpPledgeCardFollowUps(donor);
        createFollowUp(donor, latestOutstandingPledge(donor),
                "Donor replied NEEDS TIME (2) — check back in a week", Duration.ofDays(7));
        return "No problem, " + donor.getFullName() + " — we'll check back with you in a week. JazakAllah khair!";
    }

    private String wantsHuman(Donor donor) {
        createFollowUp(donor, latestOutstandingPledge(donor),
                "Donor replied TALK TO A HUMAN (3) — call them", Duration.ofDays(1));
        return "Of course — someone from the organization will call you soon, insha'Allah.";
    }

    private String createInstallmentDraft(Donor donor) {
        Pledge pledge = latestOutstandingPledge(donor);
        if (pledge == null) {
            createFollowUp(donor, null, "Donor asked about installments but has no open pledge",
                    Duration.ofDays(1));
            return "A volunteer will contact you to set up an installment plan. JazakAllah khair!";
        }
        BigDecimal outstanding = pledge.getAmount().subtract(
                pledge.getCollectedAmount() != null ? pledge.getCollectedAmount() : BigDecimal.ZERO);
        BigDecimal per = outstanding.divide(BigDecimal.valueOf(DEFAULT_INSTALLMENTS), 2, RoundingMode.CEILING);
        LocalDate due = LocalDate.now().plusMonths(1);
        for (int i = 0; i < DEFAULT_INSTALLMENTS; i++) {
            BigDecimal amount = i == DEFAULT_INSTALLMENTS - 1
                    ? outstanding.subtract(per.multiply(BigDecimal.valueOf(DEFAULT_INSTALLMENTS - 1)))
                    : per;
            if (amount.signum() <= 0) break;
            PledgeSchedule schedule = new PledgeSchedule();
            schedule.setOrganizationId(donor.getOrganizationId());
            schedule.setPledgeId(pledge.getId());
            schedule.setDueDate(due.plusMonths(i));
            schedule.setAmountDue(amount);
            pledgeScheduleRepository.save(schedule);
        }
        createFollowUp(donor, pledge,
                "Donor requested installments — draft schedule created, please confirm with them",
                Duration.ofDays(2));
        return "We've drafted a " + DEFAULT_INSTALLMENTS + "-month installment plan of about $" + per
                + "/month for your pledge. A volunteer will confirm the details with you.";
    }

    private Pledge latestOutstandingPledge(Donor donor) {
        return pledgeRepository.findByOrganizationIdAndDonorId(donor.getOrganizationId(), donor.getId()).stream()
                .filter(p -> !"fulfilled".equals(p.getStatus()) && !"cancelled".equals(p.getStatus()))
                .max(Comparator.comparing(Pledge::getCreatedAt))
                .orElse(null);
    }

    private void bumpPledgeCardFollowUps(Donor donor) {
        pledgeCardRepository.findByOrganizationIdAndDonorIdAndVerificationStatus(
                        donor.getOrganizationId(), donor.getId(), "pending")
                .forEach(card -> {
                    card.setFollowUpCount(card.getFollowUpCount() + 1);
                    pledgeCardRepository.save(card);
                });
    }

    private void createFollowUp(Donor donor, Pledge pledge, String note, Duration due) {
        FollowUp followUp = new FollowUp();
        followUp.setOrganizationId(donor.getOrganizationId());
        followUp.setDonorId(donor.getId());
        followUp.setCampaignId(pledge != null ? pledge.getCampaignId() : null);
        followUp.setAssignedToUserId(donor.getAssignedToUserId());
        followUp.setDueAt(Instant.now().plus(due));
        followUp.setNotes(note);
        followUpRepository.save(followUp);
    }

    private void recordInbound(Donor donor, String channel, String from, String body, String sid) {
        if (donor == null) {
            return; // No org to attribute the message to.
        }
        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(donor.getOrganizationId());
        message.setChannel(channel);
        message.setRecipient(from != null ? from : "");
        message.setDonorId(donor.getId());
        message.setBody(body == null || body.isBlank() ? "(empty message)" : body);
        message.setDirection("inbound");
        message.setStatus("received");
        message.setExternalId(sid);
        message.setSentAt(Instant.now());
        messageRepository.save(message);
    }
}
