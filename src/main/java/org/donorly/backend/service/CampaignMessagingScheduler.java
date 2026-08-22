package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.model.Campaign;
import org.donorly.backend.model.CampaignMessaging;
import org.donorly.backend.model.CommunicationMessage;
import org.donorly.backend.model.Donor;
import org.donorly.backend.repository.CampaignMessagingRepository;
import org.donorly.backend.repository.CampaignRepository;
import org.donorly.backend.repository.CampaignTargetRepository;
import org.donorly.backend.repository.CommunicationMessageRepository;
import org.donorly.backend.repository.DonorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Automated campaign messaging: on the configured frequency, sends the
 * campaign's message to every targeted donor over the selected channels.
 *
 * <p>Runs platform-wide (no tenant context) — only <em>active</em> campaigns
 * with a configured message and at least one channel are picked up. Email goes
 * out via SMTP; SMS/WhatsApp/robocall go via Twilio, and are recorded as
 * skipped while Twilio is in placeholder mode (email-only fallback).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignMessagingRepository messagingRepository;
    private final CampaignTargetRepository targetRepository;
    private final DonorRepository donorRepository;
    private final CommunicationMessageRepository messageRepository;
    private final MessageDeliveryService deliveryService;

    /** Hourly sweep; per-campaign frequency gating keeps actual sends on schedule. */
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void sendScheduledCampaignMessages() {
        List<Campaign> active = campaignRepository.findByStatus("active");
        for (Campaign campaign : active) {
            try {
                messagingRepository.findById(campaign.getId())
                        .filter(this::isConfigured)
                        .filter(this::isDue)
                        .ifPresent(config -> sendForCampaign(campaign, config));
            } catch (Exception e) {
                log.error("[CampaignMessaging] Failed for campaign {}: {}", campaign.getId(), e.getMessage(), e);
            }
        }
    }

    private boolean isConfigured(CampaignMessaging config) {
        return config.getMessageContent() != null && !config.getMessageContent().isBlank()
                && !config.getChannelList().isEmpty();
    }

    private boolean isDue(CampaignMessaging config) {
        if (config.getLastSentAt() == null) {
            return true;
        }
        Duration interval = switch (config.getFrequency()) {
            case "daily" -> Duration.ofDays(1);
            case "every_2_days" -> Duration.ofDays(2);
            default -> Duration.ofDays(7); // weekly
        };
        // Small tolerance so an hourly sweep doesn't drift the schedule forward each cycle.
        return config.getLastSentAt().plus(interval).minus(Duration.ofMinutes(30)).isBefore(Instant.now());
    }

    private void sendForCampaign(Campaign campaign, CampaignMessaging config) {
        List<UUID> donorIds = targetRepository.resolveTargetedDonorIds(
                campaign.getId(), campaign.getOrganizationId());
        if (donorIds.isEmpty()) {
            log.info("[CampaignMessaging] Campaign {} has no targeted donors — skipping", campaign.getId());
            return;
        }
        List<Donor> donors = donorRepository.findAllById(donorIds);
        int sent = 0;
        for (Donor donor : donors) {
            if (!"active".equals(donor.getStatus())) {
                continue; // respects do_not_contact / inactive / merged
            }
            String body = personalize(config, campaign, donor);
            for (String channel : config.getChannelList()) {
                recordAndSend(campaign, donor, channel, body);
            }
            sent++;
        }
        config.setLastSentAt(Instant.now());
        messagingRepository.save(config);
        log.info("[CampaignMessaging] Campaign {} messaged {} donors over {}",
                campaign.getId(), sent, config.getChannels());
    }

    private String personalize(CampaignMessaging config, Campaign campaign, Donor donor) {
        String content = config.getMessageContent();
        String name = config.isPersonalized() && donor.getFullName() != null ? donor.getFullName() : "friend";
        String body = content
                .replace("{{donor_name}}", name)
                .replace("{{campaign_name}}", campaign.getName())
                .replace("{{payment_link}}", config.getPaymentLink() != null ? config.getPaymentLink() : "");
        if (config.getFlyerUrl() != null && !config.getFlyerUrl().isBlank()) {
            body = body + "\n\nFlyer: " + config.getFlyerUrl();
        }
        return body;
    }

    private void recordAndSend(Campaign campaign, Donor donor, String channel, String body) {
        String recipient = "email".equals(channel) ? donor.getEmail() : donor.getPhone();

        CommunicationMessage message = new CommunicationMessage();
        message.setOrganizationId(campaign.getOrganizationId());
        message.setCampaignId(campaign.getId());
        message.setChannel(channel);
        message.setRecipient(recipient != null ? recipient : "");
        message.setDonorId(donor.getId());
        message.setSubject("email".equals(channel) ? campaign.getName() : null);
        message.setBody(body);
        message.setDirection("outbound");

        if (recipient == null || recipient.isBlank()) {
            message.setStatus("skipped");
            message.setErrorMessage("email".equals(channel) ? "Donor has no email" : "Donor has no phone");
            messageRepository.save(message);
            return;
        }

        var result = deliveryService.deliver(channel, recipient, campaign.getName(), body);
        if (result.success()) {
            message.setStatus("sent");
            message.setSentAt(Instant.now());
            message.setExternalId(result.externalId());
        } else if (result.skipped()) {
            message.setStatus("skipped");
            message.setErrorMessage(result.errorMessage());
        } else {
            message.setStatus("failed");
            message.setErrorMessage(result.errorMessage());
        }
        messageRepository.save(message);
    }
}
