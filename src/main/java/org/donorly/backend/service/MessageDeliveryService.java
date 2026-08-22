package org.donorly.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Delivery adapter for the communications module.
 * Email goes out through SMTP (same account as invitation mail); SMS, WhatsApp,
 * and robocalls go through {@link TwilioGateway}. When Twilio runs in
 * placeholder mode those channels are recorded as skipped so campaign sends
 * fall back to email-only without losing history.
 */
@Service
@Slf4j
public class MessageDeliveryService {

    private final JavaMailSender mailSender;
    private final TwilioGateway twilioGateway;

    @Value("${donorly.mail.from-address}")
    private String fromAddress;

    @Value("${donorly.mail.from-name:Donorly}")
    private String fromName;

    public MessageDeliveryService(JavaMailSender mailSender, TwilioGateway twilioGateway) {
        this.mailSender = mailSender;
        this.twilioGateway = twilioGateway;
    }

    public DeliveryResult deliver(String channel, String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            return DeliveryResult.failed("No recipient address");
        }
        var parsed = org.donorly.backend.model.CommunicationChannel.fromValue(channel);
        if (parsed == null) {
            return DeliveryResult.failed("Unsupported channel: " + channel);
        }
        return switch (parsed) {
            case EMAIL -> sendEmail(recipient, subject, body);
            case SMS -> viaTwilio("SMS", () -> twilioGateway.sendSms(recipient, body));
            case WHATSAPP -> viaTwilio("WhatsApp", () -> twilioGateway.sendWhatsApp(recipient, body));
            case ROBOCALL -> viaTwilio("Robocall", () -> twilioGateway.robocall(recipient, body));
        };
    }

    private DeliveryResult viaTwilio(String label, java.util.function.Supplier<String> send) {
        if (!twilioGateway.isLive()) {
            // Placeholder mode: keep the record, mark it skipped.
            log.info("[COMM-{}] Twilio not configured — message skipped", label);
            return DeliveryResult.skipped("Twilio live account is not active");
        }
        try {
            String sid = send.get();
            log.info("[COMM-{}] sent via Twilio", label);
            return DeliveryResult.sent(sid);
        } catch (Exception e) {
            log.error("[COMM-{}] delivery failed: {}", label, e.getMessage());
            return DeliveryResult.failed(e.getMessage());
        }
    }

    private DeliveryResult sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject != null && !subject.isBlank() ? subject : "Message from " + fromName);
            helper.setText(body != null ? body : "", false);
            mailSender.send(message);
            log.info("[COMM-EMAIL] sent, subject={}", subject);
            return DeliveryResult.sent();
        } catch (Exception e) {
            log.error("[COMM-EMAIL] delivery failed: {}", e.getMessage());
            return DeliveryResult.failed(e.getMessage());
        }
    }

    public record DeliveryResult(boolean success, boolean skipped, String errorMessage, String externalId) {
        static DeliveryResult sent() {
            return new DeliveryResult(true, false, null, null);
        }

        static DeliveryResult sent(String externalId) {
            return new DeliveryResult(true, false, null, externalId);
        }

        static DeliveryResult skipped(String reason) {
            return new DeliveryResult(false, true, reason, null);
        }

        static DeliveryResult failed(String message) {
            return new DeliveryResult(false, false, message, null);
        }
    }
}
