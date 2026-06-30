package org.donorly.donorly_backend.service;

import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DonorService {

    @Autowired
    private DonorRepository donorRepository;

    public List<Donor> getAllDonors() {
        return donorRepository.findAll();
    }

    public Optional<Donor> getDonorById(String id) {
        return donorRepository.findById(id);
    }

    public List<Donor> getDonorsByAmbassador(String ambassadorId) {
        return donorRepository.findByAmbassadorId(ambassadorId);
    }

    public Donor createDonor(Donor donor) {
        donor.setCreatedAt(LocalDateTime.now());
        return donorRepository.save(donor);
    }

    public Donor updateDonor(String id, Donor updated) {
        updated.setId(id);
        return donorRepository.save(updated);
    }

    public void deleteDonor(String id) {
        donorRepository.deleteById(id);
    }
}
