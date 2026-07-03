package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.DonationBox;
import org.donorly.donorly_backend.repository.DonationBoxRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DonationBoxService {

    private final DonationBoxRepository donationBoxRepository;

    public List<DonationBox> getAll() {
        return donationBoxRepository.findAll();
    }

    public Optional<DonationBox> getById(String id) {
        return donationBoxRepository.findById(id);
    }

    public List<DonationBox> getByAmbassadorId(String ambassadorId) {
        return donationBoxRepository.findByAssignedAmbassadorId(ambassadorId);
    }

    public DonationBox save(DonationBox donationBox) {
        return donationBoxRepository.save(donationBox);
    }

    public void delete(String id) {
        donationBoxRepository.deleteById(id);
    }
}
