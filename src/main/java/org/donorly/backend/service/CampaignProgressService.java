package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One place for the pledged/collected/count aggregate of a campaign, used by the
 * campaign dashboard, the projector live view, and the public thermometer
 * (previously three copies of the same three queries).
 */
@Service
@RequiredArgsConstructor
public class CampaignProgressService {

    private final PledgeRepository pledgeRepository;

    public record Progress(BigDecimal pledged, BigDecimal collected, int pledgeCount) {
        public BigDecimal outstanding() {
            return pledged.subtract(collected);
        }
    }

    public Progress progress(UUID orgId, UUID campaignId) {
        BigDecimal pledged = nz(pledgeRepository.sumPledgedByCampaign(orgId, campaignId));
        BigDecimal collected = nz(pledgeRepository.sumCollectedByCampaign(orgId, campaignId));
        int count = pledgeRepository.findByOrganizationIdAndCampaignId(orgId, campaignId).size();
        return new Progress(pledged, collected, count);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
