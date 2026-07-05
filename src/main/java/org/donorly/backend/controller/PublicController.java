package org.donorly.backend.controller;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.dto.PublicCheckinInfo;
import org.donorly.backend.dto.PublicThermometerResponse;
import org.donorly.backend.service.PublicPortalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/checkin/{eventId}/{code}")
    public PublicCheckinInfo checkinInfo(@PathVariable UUID eventId, @PathVariable String code) {
        return publicPortalService.checkinInfo(eventId, code);
    }

    @PostMapping("/checkin/{eventId}/{code}")
    public PublicCheckinInfo selfCheckIn(@PathVariable UUID eventId, @PathVariable String code) {
        return publicPortalService.selfCheckIn(eventId, code);
    }
}
