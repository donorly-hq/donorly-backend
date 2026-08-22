package org.donorly.backend.dto;

import org.donorly.backend.model.CampaignMessaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignMessagingResponse(
        UUID campaignId,
        String messageContent,
        String flyerUrl,
        String paymentLink,
        String frequency,
        List<String> channels,
        boolean personalized,
        Instant lastSentAt
) {
    public static CampaignMessagingResponse from(CampaignMessaging m) {
        return new CampaignMessagingResponse(
                m.getCampaignId(), m.getMessageContent(), m.getFlyerUrl(), m.getPaymentLink(),
                m.getFrequency(), m.getChannelList(), m.isPersonalized(), m.getLastSentAt());
    }
}
