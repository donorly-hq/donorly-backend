package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.PledgeCard;
import org.donorly.donorly_backend.repository.PledgeCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PledgeCardService {

    @Autowired
    private PledgeCardRepository pledgeCardRepository;

    public List<PledgeCard> getAllPledgeCards() {
        return pledgeCardRepository.findAll();
    }

    public List<PledgeCard> getUnprocessedCards() {
        return pledgeCardRepository.findByStatus("unprocessed");
    }

    public Optional<PledgeCard> getPledgeCardById(String id) {
        return pledgeCardRepository.findById(id);
    }

    public PledgeCard createPledgeCard(PledgeCard pledgeCard) {
        pledgeCard.setStatus("unprocessed");
        pledgeCard.setCreatedAt(LocalDateTime.now());
        return pledgeCardRepository.save(pledgeCard);
    }

    public PledgeCard updatePledgeCard(String id, PledgeCard updated) {
        updated.setId(id);
        return pledgeCardRepository.save(updated);
    }

    public void deletePledgeCard(String id) {
        pledgeCardRepository.deleteById(id);
    }
}
