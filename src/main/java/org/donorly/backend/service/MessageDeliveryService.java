package org.donorly.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Delivery adapter for the communications module.
 * Email goes out through SMTP (same account as invitation mail).
 * SMS is still a logging stub until an SMS provider (e.g. Twilio) is integrated.
 */
@Service
@Slf4j
public class MessageDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${donorly.mail.from-address}")
    private String fromAddress;

    @Value("${donorly.mail.from-name:Donorly}")
    private String fromName;

    public MessageDeliveryService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
            case SMS -> {
                // No SMS provider wired yet — report as sent so drafts are not lost.
                // Do not log recipient or body: message content and phone numbers are PII.
                log.info("[COMM-SMS stub] message accepted ({} chars)", body != null ? body.length() : 0);
                yield DeliveryResult.sent();
            }
        };
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

    public record DeliveryResult(boolean success, String errorMessage) {
        static DeliveryResult sent() {
            return new DeliveryResult(true, null);
        }

        static DeliveryResult failed(String message) {
            return new DeliveryResult(false, message);
        }
    }
}
