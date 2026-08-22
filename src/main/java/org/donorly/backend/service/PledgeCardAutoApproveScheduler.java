package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.donorly.backend.model.OrganizationSettings;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.OrganizationSettingsRepository;
import org.donorly.backend.repository.PledgeCardRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sweeps pledge cards sitting in the pending queue past each organization's
 * auto-approve window (default 24h). Approvable cards become real pledges;
 * cards missing campaign/donor/amount are parked in needs_verification so a
 * human resolves them instead of silently approving bad data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PledgeCardAutoApproveScheduler {

    private final PledgeCardRepository pledgeCardRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final PledgeCardService pledgeCardService;

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT2M")
    @Transactional
    public void autoApprovePendingCards() {
        Map<java.util.UUID, OrganizationSettings> settingsByOrg = settingsRepository.findAll().stream()
                .collect(Collectors.toMap(OrganizationSettings::getOrganizationId, Function.identity()));

        // Widest possible cutoff first, then apply each org's own window.
        Instant widestCutoff = Instant.now();
        List<PledgeCard> stale = pledgeCardRepository
                .findByVerificationStatusAndPendingSinceBefore("pending", widestCutoff);

        int approved = 0;
        int parked = 0;
        for (PledgeCard card : stale) {
            OrganizationSettings settings = settingsByOrg.get(card.getOrganizationId());
            if (settings == null || !settings.isPledgeCardAutoApprove()) {
                continue;
            }
            Instant orgCutoff = Instant.now()
                    .minus(Duration.ofHours(Math.max(1, settings.getPledgeCardAutoApproveHours())));
            if (card.getPendingSince() == null || card.getPendingSince().isAfter(orgCutoff)) {
                continue;
            }
            Pledge pledge = pledgeCardService.approveAndConvert(card, false);
            if (pledge != null) {
                approved++;
            } else {
                card.setVerificationStatus("needs_verification");
                parked++;
            }
            pledgeCardRepository.save(card);
        }
        if (approved > 0 || parked > 0) {
            log.info("Pledge-card auto-approve: {} approved (converted to pledges), {} parked for verification",
                    approved, parked);
        }
    }
}
