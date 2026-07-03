package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.model.Pledge;
import org.donorly.donorly_backend.repository.DonorRepository;
import org.donorly.donorly_backend.repository.PledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final PledgeRepository pledgeRepository;

    public List<Donor> getAll() {
        return donorRepository.findAll();
    }

    public Optional<Donor> getById(UUID id) {
        return donorRepository.findById(id);
    }

    /**
     * Donor no longer has a direct ambassadorId field — that link now
     * lives on Pledge. This finds every donor with at least one pledge
     * assigned to this ambassador (may return duplicates removed via
     * distinct donorId).
     */
    public List<Donor> getByAmbassadorId(UUID ambassadorId) {
        List<UUID> donorIds = pledgeRepository.findByAmbassadorId(ambassadorId)
                .stream()
                .map(Pledge::getDonorId)
                .distinct()
                .collect(Collectors.toList());
        return donorRepository.findAllById(donorIds);
    }

    public Donor save(Donor donor) {
        return donorRepository.save(donor);
    }

    public void delete(UUID id) {
        donorRepository.deleteById(id);
    }
}
