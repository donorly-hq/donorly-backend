package org.donorly.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Development delivery adapter. Logs outbound messages and treats them as sent.
 * Replace with SendGrid / Twilio / GCP integrations in production.
 */
@Service
@Slf4j
public class MessageDeliveryService {

    public DeliveryResult deliver(String channel, String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            return DeliveryResult.failed("No recipient address");
        }
        if ("email".equals(channel)) {
            log.info("[COMM-EMAIL] to={} subject={} body={}", recipient, subject, truncate(body));
            return DeliveryResult.sent();
        }
        if ("sms".equals(channel)) {
            log.info("[COMM-SMS] to={} body={}", recipient, truncate(body));
            return DeliveryResult.sent();
        }
        return DeliveryResult.failed("Unsupported channel: " + channel);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 120 ? text.substring(0, 120) + "…" : text;
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
