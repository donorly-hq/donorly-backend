package org.donorly.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin wrapper around the Stripe REST API (no SDK — same raw-HTTP pattern as
 * {@link AiGateway}). Runs in placeholder mode until {@code STRIPE_SECRET_KEY}
 * is configured: {@link #isLive()} returns false and the UI shows a
 * "live account not active" notice while manual payment entry keeps working.
 */
@Component
@Slf4j
public class StripeGateway {

    private static final String CHECKOUT_SESSIONS_URL = "https://api.stripe.com/v1/checkout/sessions";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    /** Reject webhook events whose signature timestamp is older than this (replay protection). */
    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;

    private final String secretKey;
    private final String webhookSecret;
    private final HttpClient http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeGateway(
            @Value("${donorly.stripe.secret-key:}") String secretKey,
            @Value("${donorly.stripe.webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    public boolean isLive() {
        return !secretKey.isBlank();
    }

    /**
     * Creates a Stripe Checkout session for a one-off donation and returns the
     * hosted payment page URL. Metadata carries our org/campaign/donor context
     * so the webhook can record the payment against the right records.
     */
    public String createCheckoutSession(String description,
                                        long amountCents,
                                        String currency,
                                        String successUrl,
                                        String cancelUrl,
                                        String customerEmail,
                                        Map<String, String> metadata) {
        if (!isLive()) {
            throw new IllegalStateException("Stripe is not configured (placeholder mode)");
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", successUrl);
        form.put("cancel_url", cancelUrl);
        form.put("line_items[0][price_data][currency]", currency.toLowerCase());
        form.put("line_items[0][price_data][product_data][name]", description);
        form.put("line_items[0][price_data][unit_amount]", String.valueOf(amountCents));
        form.put("line_items[0][quantity]", "1");
        if (customerEmail != null && !customerEmail.isBlank()) {
            form.put("customer_email", customerEmail.trim());
        }
        metadata.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                form.put("metadata[" + k + "]", v);
            }
        });

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHECKOUT_SESSIONS_URL))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("[Stripe] Checkout session failed: HTTP {} — {}", response.statusCode(), response.body());
                throw new IllegalStateException("Stripe checkout could not be created");
            }
            JsonNode json = objectMapper.readTree(response.body());
            return json.path("url").asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Stripe] Error calling Stripe API: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to reach Stripe right now");
        }
    }

    /**
     * Verifies a {@code Stripe-Signature} header (format {@code t=...,v1=...})
     * against the raw payload using HMAC-SHA256, per Stripe's webhook spec.
     * When no webhook secret is configured, verification is skipped (dev mode).
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret.isBlank()) {
            log.warn("[Stripe] STRIPE_WEBHOOK_SECRET not set — accepting webhook without verification (dev only)");
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String timestamp = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("t".equals(kv[0])) timestamp = kv[1];
            if ("v1".equals(kv[0])) signature = kv[1];
        }
        if (timestamp == null || signature == null) {
            return false;
        }
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(java.time.Instant.now().getEpochSecond() - ts) > SIGNATURE_TOLERANCE_SECONDS) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            return constantTimeEquals(toHex(expected), signature);
        } catch (Exception e) {
            log.error("[Stripe] Signature verification error: {}", e.getMessage());
            return false;
        }
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

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
