package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.backend.model.DonorActionToken;
import org.donorly.backend.model.Pledge;
import org.donorly.backend.model.PledgeCard;
import org.donorly.backend.repository.DonorActionTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Mints the one-time response tokens embedded in donor reminder emails. */
@Service
@RequiredArgsConstructor
public class DonorActionTokenService {

    private static final Duration TTL = Duration.ofDays(30);

    private final DonorActionTokenRepository tokenRepository;

    public DonorActionToken createForCard(PledgeCard card) {
        DonorActionToken token = base(card.getOrganizationId(), card.getDonorId());
        token.setPledgeCardId(card.getId());
        return tokenRepository.save(token);
    }

    public DonorActionToken createForPledge(Pledge pledge) {
        DonorActionToken token = base(pledge.getOrganizationId(), pledge.getDonorId());
        token.setPledgeId(pledge.getId());
        return tokenRepository.save(token);
    }

    private DonorActionToken base(UUID orgId, UUID donorId) {
        DonorActionToken token = new DonorActionToken();
        token.setOrganizationId(orgId);
        token.setDonorId(donorId);
        token.setToken(newToken());
        token.setExpiresAt(Instant.now().plus(TTL));
        return token;
    }

    private static String newToken() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
    }
}
