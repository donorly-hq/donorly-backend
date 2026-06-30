package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.DonationBox;
import org.donorly.donorly_backend.repository.DonationBoxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonationBoxService {

    @Autowired
    private DonationBoxRepository donationBoxRepository;

    public List<DonationBox> getAllBoxes() {
        return donationBoxRepository.findAll();
    }

    public Optional<DonationBox> getBoxById(String id) {
        return donationBoxRepository.findById(id);
    }

    public List<DonationBox> getBoxesByStatus(String status) {
        return donationBoxRepository.findByStatus(status);
    }

    public DonationBox createBox(DonationBox box) {
        box.setCreatedAt(LocalDateTime.now());
        return donationBoxRepository.save(box);
    }

    public DonationBox updateBox(String id, DonationBox updated) {
        updated.setId(id);
        return donationBoxRepository.save(updated);
    }

    public void deleteBox(String id) {
        donationBoxRepository.deleteById(id);
    }
}
