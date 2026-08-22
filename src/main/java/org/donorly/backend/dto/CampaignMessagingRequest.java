package org.donorly.backend.dto;

import java.util.List;

public record CampaignMessagingRequest(
        String messageContent,
        String flyerUrl,
        String paymentLink,
        String frequency,        // daily | every_2_days | weekly
        List<String> channels,   // whatsapp | sms | robocall | email
        Boolean personalized
) {
}
