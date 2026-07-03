package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.PledgeCard;
import org.donorly.donorly_backend.repository.PledgeCardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PledgeCardService {

    private final PledgeCardRepository pledgeCardRepository;

    public List<PledgeCard> getAll() {
        return pledgeCardRepository.findAll();
    }

    public Optional<PledgeCard> getById(String id) {
        return pledgeCardRepository.findById(id);
    }

    public List<PledgeCard> getByDonorId(String donorId) {
        return pledgeCardRepository.findByDonorId(donorId);
    }

    public List<PledgeCard> getByAmbassadorId(String ambassadorId) {
        return pledgeCardRepository.findByAmbassadorId(ambassadorId);
    }

    public PledgeCard save(PledgeCard pledgeCard) {
        return pledgeCardRepository.save(pledgeCard);
    }

    public void delete(String id) {
        pledgeCardRepository.deleteById(id);
    }
}
