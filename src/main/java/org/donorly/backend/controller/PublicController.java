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
    public PublicCheckinInfo checkinInfo(@PathVariable UUID eventId, @PathVariable String code) {
        return publicPortalService.checkinInfo(eventId, code);
    }

    @PostMapping("/checkin/{eventId}/{code}")
    public PublicCheckinInfo selfCheckIn(@PathVariable UUID eventId, @PathVariable String code) {
        return publicPortalService.selfCheckIn(eventId, code);
    }
}
