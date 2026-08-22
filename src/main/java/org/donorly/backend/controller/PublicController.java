package org.donorly.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PublicCheckinInfo;
import org.donorly.backend.dto.PublicSelfPledgeRequest;
import org.donorly.backend.dto.PublicSelfPledgeResponse;
import org.donorly.backend.dto.PublicThermometerResponse;
import org.donorly.backend.service.PublicPortalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Unauthenticated endpoints — everything here must stay safe for anonymous access. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicPortalService publicPortalService;
    private final org.donorly.backend.service.StripeCheckoutService stripeCheckoutService;
    private final org.donorly.backend.service.TwilioInboundService twilioInboundService;

    /** Whether online card payments are live (drives the public "Pay now" button). */
    @GetMapping("/stripe/status")
    public java.util.Map<String, Boolean> stripeStatus() {
        return java.util.Map.of("live", stripeCheckoutService.isLive());
    }

    /** Starts a Stripe Checkout for a direct campaign donation; returns the hosted URL. */
    @PostMapping("/campaigns/{campaignId}/checkout")
    public java.util.Map<String, String> checkout(
            @PathVariable UUID campaignId,
            @Valid @RequestBody org.donorly.backend.dto.PublicCheckoutRequest request) {
        String url = stripeCheckoutService.checkout(
                campaignId, request.amount(), request.donorName(), request.donorEmail());
        return java.util.Map.of("url", url);
    }

    /** Stripe webhook — signature-verified inside the service. */
    @PostMapping("/stripe/webhook")
    public void stripeWebhook(@RequestBody String payload,
                              @org.springframework.web.bind.annotation.RequestHeader(
                                      value = "Stripe-Signature", required = false) String signature) {
        stripeCheckoutService.handleWebhook(payload, signature);
    }

    /**
     * Twilio inbound SMS/WhatsApp webhook (form-encoded). Returns TwiML so the
     * reply goes straight back to the donor over the same channel.
     */
    @PostMapping(value = "/twilio/inbound",
            consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = org.springframework.http.MediaType.APPLICATION_XML_VALUE)
    public String twilioInbound(
            @org.springframework.web.bind.annotation.RequestParam(value = "From", required = false) String from,
            @org.springframework.web.bind.annotation.RequestParam(value = "Body", required = false) String body,
            @org.springframework.web.bind.annotation.RequestParam(value = "MessageSid", required = false) String sid) {
        String reply = twilioInboundService.handleInbound(from, body, sid);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Message>"
                + escapeXml(reply) + "</Message></Response>";
    }

    private static String escapeXml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    @GetMapping("/thermometer/{campaignId}")
    public PublicThermometerResponse thermometer(@PathVariable UUID campaignId) {
        return publicPortalService.thermometer(campaignId);
    }

    /** Event Mode: a donor pledges from their own phone via the QR code. */
    @PostMapping("/pledge/{campaignId}")
    public PublicSelfPledgeResponse selfPledge(@PathVariable UUID campaignId,
                                               @Valid @RequestBody PublicSelfPledgeRequest request,
                                               HttpServletRequest http) {
        return publicPortalService.selfPledge(campaignId, request, clientIp(http));
    }

    /** Cloud Run terminates TLS at the load balancer; the caller is the first X-Forwarded-For hop. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/checkin/{eventId}/{code}")
    public PublicCheckinInfo checkinInfo(@PathVariable UUID eventId, @PathVariable String code,
                                         HttpServletRequest http) {
        return publicPortalService.checkinInfo(eventId, code, clientIp(http));
    }

    @PostMapping("/checkin/{eventId}/{code}")
    public PublicCheckinInfo selfCheckIn(@PathVariable UUID eventId, @PathVariable String code,
                                         HttpServletRequest http) {
        return publicPortalService.selfCheckIn(eventId, code, clientIp(http));
    }
}
