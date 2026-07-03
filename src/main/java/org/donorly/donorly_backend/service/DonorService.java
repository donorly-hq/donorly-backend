package org.donorly.donorly_backend.service;

import lombok.RequiredArgsConstructor;
import org.donorly.donorly_backend.model.Donor;
import org.donorly.donorly_backend.repository.DonorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;

    public List<Donor> getAll() {
        return donorRepository.findAll();
    }

    public Optional<Donor> getById(String id) {
        return donorRepository.findById(id);
    }

    public List<Donor> getByAmbassadorId(String ambassadorId) {
        return donorRepository.findByAmbassadorId(ambassadorId);
    }

    public Donor save(Donor donor) {
        return donorRepository.save(donor);
    }

    public void delete(String id) {
        donorRepository.deleteById(id);
    }
}
