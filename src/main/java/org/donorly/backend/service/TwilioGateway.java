package org.donorly.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin wrapper around the Twilio REST API for WhatsApp, SMS, and robocalls
 * (voice with inline TwiML). Same raw-HTTP + placeholder-mode pattern as
 * {@link StripeGateway}: until the {@code TWILIO_*} secrets are configured,
 * {@link #isLive()} is false and callers fall back to email-only sending with
 * a "live account not active" notice in the UI.
 */
@Component
@Slf4j
public class TwilioGateway {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final String accountSid;
    private final String authToken;
    private final String whatsappFrom;
    private final String smsFrom;
    private final String voiceFrom;
    private final HttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TwilioGateway(
            @Value("${donorly.twilio.account-sid:}") String accountSid,
            @Value("${donorly.twilio.auth-token:}") String authToken,
            @Value("${donorly.twilio.whatsapp-from:}") String whatsappFrom,
            @Value("${donorly.twilio.sms-from:}") String smsFrom,
            @Value("${donorly.twilio.voice-from:}") String voiceFrom) {
        this.accountSid = trim(accountSid);
        this.authToken = trim(authToken);
        this.whatsappFrom = trim(whatsappFrom);
        this.smsFrom = trim(smsFrom);
        this.voiceFrom = trim(voiceFrom);
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    public boolean isLive() {
        return !accountSid.isBlank() && !authToken.isBlank();
    }

    /** Sends a WhatsApp message; returns the Twilio message SID. */
    public String sendWhatsApp(String to, String body) {
        return sendMessage("whatsapp:" + normalize(to), "whatsapp:" + whatsappFrom, body);
    }

    /** Sends an SMS; returns the Twilio message SID. */
    public String sendSms(String to, String body) {
        return sendMessage(normalize(to), smsFrom, body);
    }

    /**
     * Places a robocall that reads {@code message} aloud (text-to-speech via
     * inline TwiML); returns the Twilio call SID.
     */
    public String robocall(String to, String message) {
        requireLive();
        String twiml = "<Response><Say voice=\"alice\">" + escapeXml(message) + "</Say></Response>";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", normalize(to));
        form.put("From", voiceFrom);
        form.put("Twiml", twiml);
        return post("Calls.json", form);
    }

    private String sendMessage(String to, String from, String body) {
        requireLive();
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", to);
        form.put("From", from);
        form.put("Body", body);
        return post("Messages.json", form);
    }

    private String post(String resource, Map<String, String> form) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/" + resource;
            String auth = Base64.getEncoder().encodeToString(
                    (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("[Twilio] {} failed: HTTP {} — {}", resource, response.statusCode(), response.body());
                throw new IllegalStateException("Twilio request failed");
            }
            JsonNode json = objectMapper.readTree(response.body());
            return json.path("sid").asText(null);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Twilio] Error calling Twilio API: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to reach Twilio right now");
        }
    }

    private void requireLive() {
        if (!isLive()) {
            throw new IllegalStateException("Twilio is not configured (placeholder mode)");
        }
    }

    /** Twilio wants E.164; strip everything but digits and a leading +, default to US +1. */
    static String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.length() == 10) return "+1" + digits;
        return "+" + digits;
    }

    private static String encodeForm(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        form.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static String escapeXml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
